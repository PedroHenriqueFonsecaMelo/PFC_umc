package umc.exs.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.daos.mappers.ClienteMapper;
import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.daos.repository.RecuperacaoSenhaRepository;
import umc.exs.model.daos.repository.TransacaoRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.foundation.RecuperacaoSenha;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.service.email.EmailService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CartaoRepository cartaoRepository;
    private final EnderecoRepository enderecoRepository;
    private final TransacaoRepository transacaoRepository;
    private final RecuperacaoSenhaRepository recuperacaoSenhaRepository;
    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;
    private final EnderecoService enderecoService;
    private final CartaoService cartaoService;
    private final ClienteMapper clienteMapper;
    private final EnderecoMapper enderecoMapper;

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    // --- MÉTODOS DE COMPRA E CARTEIRA ---

    public boolean possuiCartaoCadastrado(String email) {
        return clienteRepository.findByEmail(email)
                .map(c -> c.getCartoes() != null && !c.getCartoes().isEmpty())
                .orElse(false);
    }

    @Transactional
    public ClienteDTO adicionarTokens(Long clienteId, Double valor, String metodo, String numCartao) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        double saldoAnterior = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
        cliente.setSaldoTokens(saldoAnterior + valor);

        Cliente salvo = clienteRepository.save(cliente);

        String finalCartao = (metodo.equalsIgnoreCase("CARTAO") && numCartao != null && numCartao.length() >= 4)
                ? numCartao.substring(numCartao.length() - 4)
                : "N/A";

        Transacao transacao = Transacao.builder()
                .cliente(cliente)
                .valor(valor)
                .dataHora(LocalDateTime.now())
                .metodoPagamento(metodo)
                .finalCartao(finalCartao)
                .build();

        transacaoRepository.save(transacao);

        log.info("Transação aprovada: Cliente ID {} | Valor {} | Método {}", clienteId, valor, metodo);

        return clienteMapper.toDTO(salvo);
    }

    public List<Transacao> listarHistoricoTransacoes(Long clienteId) {
        return transacaoRepository.findByClienteIdOrderByDataHoraDesc(clienteId);
    }

    public List<Transacao> listarHistoricoTransacoes(String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Previne o erro de Unboxing garantindo que o ID seja usado como objeto Long
        Long id = cliente.getId();
        if (id == null) throw new RuntimeException("ID do cliente é nulo.");
        
        return transacaoRepository.findByClienteIdOrderByDataHoraDesc(id);
    }

    // --- MÉTODOS DE BUSCA E ENTIDADE ---

    public Optional<Cliente> buscarEntidadePorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        return clienteRepository.findByEmail(email).map(clienteMapper::toDTO);
    }

    // --- MÉTODOS DE CADASTRO E ATUALIZAÇÃO ---

    @Transactional
    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        validarDadosSignup(signupDTO);
        Cliente cliente = clienteMapper.toEntity(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        cliente.setSaldoTokens(0.0);
        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.toDTO(salvo);
    }

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO dto) {
        Cliente clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        clienteExistente.setNome(dto.getNome());
        clienteExistente.setDatanasc(dto.getDatanasc());

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            clienteExistente.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        atualizarEnderecosManualmente(clienteExistente, dto.getEnderecos());
        atualizarCartoesManualmente(clienteExistente, dto.getCartoes());

        Cliente salvo = clienteRepository.save(clienteExistente);
        return clienteMapper.toDTO(salvo);
    }

    private void atualizarEnderecosManualmente(Cliente cliente, List<EnderecoDTO> dtos) {
        Set<Endereco> novosEnderecos = new HashSet<>();
        if (dtos != null) {
            for (EnderecoDTO endDto : dtos) {
                Endereco endereco;
                if (endDto.getId() != null && endDto.getId() != 0) {
                    endereco = enderecoRepository.findById(endDto.getId()).orElse(null);
                    if (endereco != null)
                        enderecoMapper.updateEntityFromDto(endDto, endereco);
                } else {
                    endereco = enderecoService.saveOrReuseEndereco(endDto);
                    endereco.getClientes().add(cliente);
                }
                if (endereco != null)
                    novosEnderecos.add(endereco);
            }
        }
        cliente.setEnderecos(novosEnderecos);
    }

    private void atualizarCartoesManualmente(Cliente cliente, List<CartaoDTO> dtos) {
        Set<Cartao> novosCartoes = new HashSet<>();
        if (dtos != null) {
            for (CartaoDTO cartaoDto : dtos) {
                Cartao cartao;
                if (cartaoDto.getId() != null && cartaoDto.getId() != 0) {
                    cartao = cartaoRepository.findById(cartaoDto.getId()).orElse(null);
                } else {
                    cartao = cartaoService.saveOrReuseCartao(cartaoDto);
                    cartao.getClientes().add(cliente);
                }
                if (cartao != null)
                    novosCartoes.add(cartao);
            }
        }
        cliente.setCartoes(novosCartoes);
    }

    private void validarDadosSignup(SignupDTO dto) {
        if (clienteRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        if (dto.getSenha().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres.");
        }
    }

    public Optional<ClienteDTO> autenticarCliente(String email, String senha) {
        return clienteRepository.findByEmail(email)
                .filter(c -> passwordEncoder.matches(senha, c.getSenha()))
                .map(clienteMapper::toDTO);
    }

    @Transactional
    public void iniciarRecuperacaoSenha(String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email não encontrado."));

        // Limpa solicitações anteriores para este cliente
        recuperacaoSenhaRepository.deleteByCliente(cliente);

        String token = gerarTokenManual();
        LocalDateTime validade = LocalDateTime.now().plusMinutes(30);

        RecuperacaoSenha rec = new RecuperacaoSenha(token, cliente, validade);
        rec.setEmail(email);
        recuperacaoSenhaRepository.save(rec);

        String link = baseUrl + "/clientes/reset-senha?token=" + token;

        log.info("Link de recuperação gerado para {}: {}", email, link);

        String assunto = "Recuperação de Senha";
        String texto = "Olá,\n\nClique no link abaixo para redefinir sua senha:\n\n" +
                link + "\n\nSe você não solicitou, apenas ignore este e-mail.";

        emailService.enviar(email, assunto, texto);
    }

    private String gerarTokenManual() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional(readOnly = true)
    public boolean validarTokenRecuperacao(String token) {
        return recuperacaoSenhaRepository.findByToken(token)
                .map(rec -> LocalDateTime.now().isBefore(rec.getDataExpiracao()))
                .orElse(false);
    }

    @Transactional
    public String alterarSenhaComToken(String token, String novaSenha) {
        RecuperacaoSenha rec = recuperacaoSenhaRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));

        if (LocalDateTime.now().isAfter(rec.getDataExpiracao())) {
            throw new IllegalArgumentException("Token expirado.");
        }

        Cliente cliente = rec.getCliente();
        cliente.setSenha(passwordEncoder.encode(novaSenha));
        clienteRepository.save(cliente);

        recuperacaoSenhaRepository.delete(rec);

        return cliente.getEmail();
    }
}