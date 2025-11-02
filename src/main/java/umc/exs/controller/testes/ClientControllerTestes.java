package umc.exs.controller.testes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import umc.exs.backstage.security.JwtUtil;
import umc.exs.backstage.service.FieldValidation;
import umc.exs.model.DAO.CartaoMapper;
import umc.exs.model.DAO.ClienteMapper;
import umc.exs.model.DAO.EnderecoMapper;
import umc.exs.model.DTO.user.CartaoDTO;
import umc.exs.model.DTO.user.ClienteDTO;
import umc.exs.model.DTO.user.EnderecoDTO;
import umc.exs.model.entidades.Cartao;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.repository.CartaoRepository;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.EnderecoRepository;

@RestController
@RequestMapping("/clientestestes")
public class ClientControllerTestes {

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

        // Criar entidade e salvar
        ClienteDTO clienteDTO = new ClienteDTO();
        clienteDTO.setEmail(email);
        clienteDTO.setNome(nome);
        clienteDTO.setDatanasc(datanasc);
        clienteDTO.setGen(gen);
        clienteDTO.setSenha(passwordEncoder.encode(senha));

        Cliente clienteEntity = ClienteMapper.toEntity(clienteDTO);
        Cliente savedCliente = clienteRepository.save(clienteEntity);

        // Gerar token JWT
        String token = jwtUtil.generateToken(savedCliente.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("cliente", ClienteMapper.fromEntity(savedCliente));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Busca um cliente pelo ID.
     * Disponível apenas para usuários autenticados (USER ou ADMIN).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ClienteDTO> buscarCliente(@PathVariable Long id) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        return clienteOpt.map(cliente -> new ResponseEntity<>(ClienteMapper.fromEntity(cliente), HttpStatus.OK))
                         .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Atualiza as informações básicas de um cliente existente.
     * Requer autenticação como USER ou ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> atualizarCliente(@PathVariable Long id, @RequestBody ClienteDTO clienteDTO) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);

        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();

            // Atualizar campos do cliente
            String nome = FieldValidation.sanitize(clienteDTO.getNome());
            String dataNasc = FieldValidation.sanitize(clienteDTO.getDatanasc());
            String gen = FieldValidation.sanitize(clienteDTO.getGen());
            String email = FieldValidation.sanitizeEmail(clienteDTO.getEmail());

            if (nome != null) cliente.setNome(nome);
            if (dataNasc != null) cliente.setDatanasc(dataNasc);
            if (gen != null) cliente.setGen(gen);
            if (email != null) cliente.setEmail(email);

            Cliente updatedCliente = clienteRepository.save(cliente);
            return new ResponseEntity<>(ClienteMapper.fromEntity(updatedCliente), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Anonimiza e remove logicamente um cliente do sistema.
     * Apenas administradores têm permissão para essa operação.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Void> deletarCliente(@PathVariable Long id) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);

        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            if (!isClienteAutenticado(id)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // Anonimizar dados sensíveis e salvar alterações
            anonymizeClientData(cliente);
            anonymizeRelatedData(cliente);

            clienteRepository.save(cliente);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void anonymizeClientData(Cliente cliente) {
        // Anonimizar campos sensíveis do cliente
        cliente.setNome("ANON-" + UUID.randomUUID().toString().substring(0, 8));
        cliente.setEmail("anon+" + UUID.randomUUID().toString().substring(0, 8) + "@example.invalid");
        cliente.setDatanasc(null);
        cliente.setGen(null);
    }

    private void anonymizeRelatedData(Cliente cliente) {
        // Anonimizar endereços
        cliente.getEnderecos().forEach(endereco -> {
            endereco.setRua(null);
            endereco.setCidade(null);
            endereco.setBairro(null);
            endereco.setComplemento(null);
            endereco.setNumero("0");
            enderecoRepository.save(endereco);
        });

        // Anonimizar cartões
        cliente.getCartoes().forEach(cartao -> {
            cartao.setNumero("0");
            cartao.setCvv("0");
            cartao.setNomeTitular("ANON");
            cartaoRepository.save(cartao);
        });
    }

    private boolean isClienteAutenticado(Long clienteId) {
        String token = obterTokenDaRequisicao();
        if (token == null) {
            return false;
        }
        String clienteAutenticadoId = jwtUtil.getUserIdFromToken(token);
        return clienteAutenticadoId != null && clienteAutenticadoId.equals(clienteId.toString());
    }

    private String obterTokenDaRequisicao() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // Adicionar e buscar endereços e cartões
    @PostMapping("/{clienteId}/endereco")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> adicionarEndereco(@PathVariable Long clienteId, @RequestBody EnderecoDTO enderecoDTO) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        
        Endereco endereco = EnderecoMapper.toEntity(enderecoDTO);
        endereco.addCliente(clienteOpt.get());  // Associa o cliente ao endereço
        Endereco savedEndereco = enderecoRepository.save(endereco);

        Cliente cliente = clienteOpt.get();
        cliente.getEnderecos().add(savedEndereco);
        clienteRepository.save(cliente);

        return new ResponseEntity<>(EnderecoMapper.fromEntity(savedEndereco), HttpStatus.CREATED);
    }

    @GetMapping("/{clienteId}/enderecos")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> buscarEnderecos(@PathVariable Long clienteId) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        return clienteOpt.map(cliente -> new ResponseEntity<>(cliente.getEnderecos(), HttpStatus.OK))
                         .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/{clienteId}/cartao")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> adicionarCartao(@PathVariable Long clienteId, @RequestBody CartaoDTO cartaoDTO) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        
        Cartao cartao = CartaoMapper.toEntity(cartaoDTO);
        cartao.addCliente(clienteOpt.get());  // Associa o cliente ao cartão
        Cartao savedCartao = cartaoRepository.save(cartao);

        Cliente cliente = clienteOpt.get();
        cliente.getCartoes().add(savedCartao);
        clienteRepository.save(cliente);

        return new ResponseEntity<>(CartaoMapper.fromEntity(savedCartao), HttpStatus.CREATED);
    }

    @GetMapping("/{clienteId}/cartoes")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> buscarCartoes(@PathVariable Long clienteId) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        return clienteOpt.map(cliente -> new ResponseEntity<>(cliente.getCartoes(), HttpStatus.OK))
                         .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
