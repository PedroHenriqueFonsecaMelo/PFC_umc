package umc.exs.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import umc.exs.backstage.service.FieldValidation;
import umc.exs.model.DAO.CartaoMapper;
import umc.exs.model.DAO.ClienteMapper;
import umc.exs.model.DAO.EnderecoMapper;
import umc.exs.model.DTO.CartaoDTO;
import umc.exs.model.DTO.ClienteDTO;
import umc.exs.model.DTO.EnderecoDTO;
import umc.exs.model.entidades.Cartao;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.repository.CartaoRepository;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.security.JwtUtil;

@RestController
@RequestMapping("/clientes")
public class ClientController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    /**
     * Criar cliente (compatível com testes existentes).
     * Aceita JSON com chaves em português (ex: "senha") e retorna token JWT +
     * cliente DTO.
     */
    @PostMapping
    public ResponseEntity<?> criarCliente(@RequestBody Map<String, Object> signup) {
        String email = FieldValidation.sanitizeEmail((String) signup.get("email"));
        String nome = FieldValidation.sanitize((String) signup.get("nome"));
        String senha = FieldValidation.sanitize((String) signup.get("senha"));
        String datanasc = FieldValidation.sanitize((String) signup.get("datanasc"));
        String gen = FieldValidation.sanitize((String) signup.get("gen"));

        if (email == null || nome == null || senha == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid input"));
        }

        // Verificar email duplicado
        if (clienteRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "email already registered"));
        }

        // Criar entidade e salvar
        Cliente c = new Cliente();
        c.setEmail(email);
        c.setNome(nome);
        c.setDatanasc(datanasc);
        c.setGen(gen);
        c.setSenha(passwordEncoder.encode(senha));
        Cliente saved = clienteRepository.save(c);

        // Gerar token JWT
        String token = jwtUtil.generateToken(saved.getEmail());

        // Usar Map explícito com tipo seguro
        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("cliente", ClienteMapper.fromEntity(saved));

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Busca um cliente pelo ID.
     * Disponível apenas para usuários autenticados (USER ou ADMIN).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ClienteDTO> buscarCliente(@PathVariable Long id) {
        Optional<Cliente> cliente = clienteRepository.findById(id);
        return cliente.map(c -> new ResponseEntity<>(ClienteMapper.fromEntity(c), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Atualiza as informações básicas de um cliente existente.
     * Requer autenticação como USER ou ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> atualizarCliente(@PathVariable Long id, @RequestBody ClienteDTO clienteDto) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            String nome = FieldValidation.sanitize(clienteDto.getNome());
            String data = FieldValidation.sanitize(clienteDto.getDatanasc());
            String gen = FieldValidation.sanitize(clienteDto.getGen());
            String email = FieldValidation.sanitizeEmail(clienteDto.getEmail());

            if (nome != null)
                cliente.setNome(nome);
            if (data != null)
                cliente.setDatanasc(data);
            if (gen != null)
                cliente.setGen(gen);
            if (email != null)
                cliente.setEmail(email);

            Cliente atualizado = clienteRepository.save(cliente);
            return new ResponseEntity<>(ClienteMapper.fromEntity(atualizado), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Anonimiza e remove logicamente um cliente do sistema.
     * Apenas administradores têm permissão para essa operação.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id") // Verifica se é o próprio cliente ou admin
    public ResponseEntity<Void> deletarCliente(@PathVariable Long id) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        if (clienteOpt.isPresent()) {
            Cliente cli = clienteOpt.get();

            // Se o cliente autenticado não for o próprio cliente ou um admin, negar a
            // operação
            if (!isClienteAutenticado(id)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN); // Retorna Forbidden se não for o mesmo cliente
            }

            // Anonimizar os dados sensíveis do cliente
            anonymizeClientData(cli);

            // Anonimizar os dados relacionados
            anonymizeRelatedData(cli);

            // Salvar as alterações no banco
            clienteRepository.save(cli);

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void anonymizeClientData(Cliente cli) {
        // Anonimizar campos sensíveis do cliente
        cli.setNome("ANON-" + UUID.randomUUID().toString().substring(0, 8));
        cli.setEmail("anon+" + UUID.randomUUID().toString().substring(0, 8) + "@example.invalid");
        cli.setDatanasc(null);
        cli.setGen(null);

        // Se houver um CPF, anonimize também (se o CPF for armazenado)
        try {
            cli.getClass().getMethod("setCpf", String.class).invoke(cli, (Object) null);
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {
        }
    }

    private void anonymizeRelatedData(Cliente cli) {
        // Anonimizar endereços relacionados
        Set<Endereco> enderecos = cli.getEnderecos();
        if (enderecos != null) {
            for (Endereco e : enderecos) {
                // Não remover o histórico, mas anonimizar os dados
                e.setRua(null);
                e.setCidade(null);
                e.setBairro(null);
                e.setComplemento(null);
                e.setNumero("0");
                enderecoRepository.save(e); // Atualiza no banco sem os dados sensíveis
            }
        }

        // Anonimizar cartões relacionados
        Set<Cartao> cartoes = cli.getCartoes();
        if (cartoes != null) {
            for (Cartao cc : cartoes) {
                // Não remover o histórico, mas anonimizar os dados sensíveis
                cc.setNumero("0"); // Cartão fictício
                cc.setCvv("0"); // CVV fictício
                cc.setNomeTitular("ANON");
                cartaoRepository.save(cc); // Atualiza no banco
            }
        }
    }

    private boolean isClienteAutenticado(Long clienteId) {
        // Obter o token JWT do cabeçalho Authorization
        String token = obterTokenDaRequisicao(); // Implemente este método para pegar o token da requisição
        if (token == null) {
            return false; // Ou algum comportamento desejado se o token não for encontrado
        }

        // Agora passa o token para o método getUserIdFromToken
        String clienteAutenticadoId = jwtUtil.getUserIdFromToken(token);
        return clienteAutenticadoId != null && clienteAutenticadoId.equals(clienteId.toString());
    }

    // Exemplo de método que extrai o token da requisição
    private String obterTokenDaRequisicao() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Retorna o token sem o prefixo "Bearer "
        }
        return null;
    }

    // Relacionamento com Endereços e Cartões
    /**
     * Adiciona um novo endereço ao cliente especificado.
     * Requer autenticação como USER ou ADMIN.
     */
    @PostMapping("/{clienteId}/endereco")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> adicionarEndereco(@PathVariable Long clienteId, @RequestBody EnderecoDTO enderecoDto) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        Endereco e = EnderecoMapper.toEntity(enderecoDto);
        // Associar explicitamente o cliente ao endereço
        try {
            e.getClass().getMethod("setClienteId", Long.class).invoke(e, clienteId);
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException
                | InvocationTargetException ignored) {
        }
        Endereco salvo = enderecoRepository.save(e);
        Cliente c = clienteOpt.get();
        c.getEnderecos().add(salvo);
        clienteRepository.save(c);
        return new ResponseEntity<>(EnderecoMapper.fromEntity(salvo), HttpStatus.CREATED);
    }

    /**
     * Retorna todos os endereços associados a um cliente.
     * Requer autenticação como USER ou ADMIN.
     */
    @GetMapping("/{clienteId}/enderecos")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> buscarEnderecos(@PathVariable Long clienteId) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        return clienteOpt.map(c -> new ResponseEntity<>(c.getEnderecos(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Adiciona um novo cartão de pagamento ao cliente especificado.
     * Requer autenticação como USER ou ADMIN.
     */
    @PostMapping("/{clienteId}/cartao")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> adicionarCartao(@PathVariable Long clienteId, @RequestBody CartaoDTO cartaoDto) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        Cartao cartao = CartaoMapper.toEntity(cartaoDto);
        try {
            cartao.getClass().getMethod("setClienteId", Long.class).invoke(cartao, clienteId);
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException
                | InvocationTargetException ignored) {
        }
        Cartao salvo = cartaoRepository.save(cartao);
        Cliente c = clienteOpt.get();
        c.getCartoes().add(salvo);
        clienteRepository.save(c);
        return new ResponseEntity<>(CartaoMapper.fromEntity(salvo), HttpStatus.CREATED);
    }

    /**
     * Retorna todos os cartões associados a um cliente.
     * Requer autenticação como USER ou ADMIN.
     */
    @GetMapping("/{clienteId}/cartoes")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> buscarCartoes(@PathVariable Long clienteId) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        return clienteOpt.map(c -> new ResponseEntity<>(c.getCartoes(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
