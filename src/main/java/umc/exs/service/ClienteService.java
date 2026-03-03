package umc.exs.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import umc.exs.utils.FieldValidation;

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

    // ==========================================================
    // 🔹 CADASTRO E SALVAMENTO
    // ==========================================================

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
    public ClienteDTO salvarClienteCompleto(SignupDTO signupDTO, EnderecoDTO enderecoDTO, CartaoDTO cartaoDTO) {
        validarDadosSignup(signupDTO);
        // 1. Criar e encriptar senha
        Cliente cliente = clienteMapper.toEntity(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        cliente.setSaldoTokens(0.0);

        // 2. Salvar/Reutilizar Endereço
        Endereco endereco = enderecoService.saveOrReuseEndereco(enderecoDTO);
        cliente.getEnderecos().add(endereco);
        endereco.getClientes().add(cliente);

        // 3. Salvar/Reutilizar Cartão
        Cartao cartao = cartaoService.saveOrReuseCartao(cartaoDTO);
        cliente.getCartoes().add(cartao);
        cartao.getClientes().add(cliente);

        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.toDTO(salvo);
    }

    // ==========================================================
    // 💾 ATUALIZAÇÃO (COM LÓGICA DE REMOÇÃO DE ÓRFÃOS)
    // ==========================================================

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO dto) {
        Cliente clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // Atualiza campos básicos
        clienteExistente.setNome(FieldValidation.sanitize(dto.getNome()));
        clienteExistente.setDatanasc(dto.getDatanasc());

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            if (!FieldValidation.isValidPassword(dto.getSenha())) {
                throw new IllegalArgumentException("Nova senha inválida.");
            }
            clienteExistente.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        // Lógica para Endereços (Remove quem saiu do DTO, adiciona novos)
        atualizarEnderecosManualmente(clienteExistente, dto.getEnderecos());

        // Lógica para Cartões
        atualizarCartoesManualmente(clienteExistente, dto.getCartoes());

        Cliente salvo = clienteRepository.save(clienteExistente);
        return clienteMapper.toDTO(salvo);
    }

    private void atualizarEnderecosManualmente(Cliente cliente, List<EnderecoDTO> dtos) {
        Set<Long> idsNoDto = dtos.stream()
                .map(EnderecoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        // Desassocia os que não vieram no DTO
        cliente.getEnderecos().removeIf(e -> {
            if (!idsNoDto.contains(e.getId())) {
                e.getClientes().remove(cliente);
                return true;
            }
            return false;
        });

        // Adiciona/Atualiza os que vieram
        for (EnderecoDTO endDto : dtos) {
            Endereco endereco;
            if (endDto.getId() != null && endDto.getId() != 0) {
                endereco = enderecoRepository.findById(endDto.getId()).orElseThrow();
                enderecoMapper.updateEntityFromDto(endDto, endereco);
            } else {
                endereco = enderecoService.saveOrReuseEndereco(endDto);
                if (!endereco.getClientes().contains(cliente)) {
                    endereco.getClientes().add(cliente);
                }
            }
            cliente.getEnderecos().add(endereco);
        }
    }

    private void atualizarCartoesManualmente(Cliente cliente, List<CartaoDTO> dtos) {
        Set<Long> idsNoDto = dtos.stream()
                .map(CartaoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        cliente.getCartoes().removeIf(c -> {
            if (!idsNoDto.contains(c.getId())) {
                c.getClientes().remove(cliente);
                return true;
            }
            return false;
        });

        for (CartaoDTO cartaoDto : dtos) {
            Cartao cartao;
            if (cartaoDto.getId() != null && cartaoDto.getId() != 0) {
                cartao = cartaoRepository.findById(cartaoDto.getId()).orElseThrow();
            } else {
                cartao = cartaoService.saveOrReuseCartao(cartaoDto);
                if (!cartao.getClientes().contains(cliente)) {
                    cartao.getClientes().add(cliente);
                }
            }
            cliente.getCartoes().add(cartao);
        }
    }

    // ==========================================================
    // 🗑️ DELEÇÃO
    // ==========================================================

    @Transactional
    public void deletarClientePorId(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // Limpa M2M para evitar violação de constraint ou órfãos presos
        cliente.getEnderecos().forEach(e -> e.getClientes().remove(cliente));
        cliente.getCartoes().forEach(c -> c.getClientes().remove(cliente));

        clienteRepository.delete(cliente);
        log.info("Cliente ID {} removido com sucesso.", clienteId);
    }

    // ==========================================================
    // 🪙 CARTEIRA E TRANSAÇÕES
    // ==========================================================

    @Transactional
    public ClienteDTO adicionarTokens(Long clienteId, Double valor, String metodo, String numCartao) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        double saldoAnterior = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
        cliente.setSaldoTokens(saldoAnterior + valor);

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
        return clienteMapper.toDTO(clienteRepository.save(cliente));
    }

    public List<Transacao> listarHistoricoTransacoes(Long clienteId) {
        return transacaoRepository.findByClienteIdOrderByDataHoraDesc(clienteId);
    }

    public List<Transacao> listarHistoricoTransacoes(String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado para o e-mail: " + email));

        // Garantimos que o ID não é nulo para evitar erro de Unboxing no Repository
        Long id = cliente.getId();
        if (id == null) {
            throw new RuntimeException("Erro de integridade: ID do cliente é nulo.");
        }

        return transacaoRepository.findByClienteIdOrderByDataHoraDesc(id);
    }

    // ==========================================================
    // 🔐 AUTENTICAÇÃO E BUSCA
    // ==========================================================

    public Optional<ClienteDTO> autenticarCliente(String email, String senha) {
        return clienteRepository.findByEmail(FieldValidation.sanitizeEmail(email))
                .filter(c -> passwordEncoder.matches(senha, c.getSenha()))
                .map(clienteMapper::toDTO);
    }

    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        return clienteRepository.findByEmail(FieldValidation.sanitizeEmail(email))
                .map(clienteMapper::toDTO);
    }

    public Optional<Cliente> buscarEntidadePorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    // ==========================================================
    // 🔑 RECUPERAÇÃO DE SENHA
    // ==========================================================

    @Transactional
    public void iniciarRecuperacaoSenha(String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email não encontrado."));

        recuperacaoSenhaRepository.deleteByCliente(cliente);

        String token = UUID.randomUUID().toString().replace("-", "");
        RecuperacaoSenha rec = new RecuperacaoSenha(token, cliente, LocalDateTime.now().plusMinutes(30));
        rec.setEmail(email);
        recuperacaoSenhaRepository.save(rec);

        String link = baseUrl + "/clientes/reset-senha?token=" + token;
        emailService.enviar(email, "Recuperação de Senha", "Link: " + link);
        log.info("Token de recuperação enviado para {}", email);
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

    // --- VALIDAÇÕES ---

    private void validarDadosSignup(SignupDTO dto) {
        if (!FieldValidation.validarCampos(dto))
            throw new IllegalArgumentException("Campos obrigatórios ausentes.");
        String safeEmail = FieldValidation.sanitizeEmail(dto.getEmail());
        if (clienteRepository.findByEmail(safeEmail).isPresent())
            throw new IllegalArgumentException("Email já existe.");
        dto.setEmail(safeEmail);

        if (!FieldValidation.isValidCPF(dto.getCpf()))
            throw new IllegalArgumentException("CPF inválido.");

        LocalDate dataNasc = FieldValidation.isValidBirthDate(dto.getDatanasc());
        if (dataNasc == null || !FieldValidation.isOver18(dataNasc))
            throw new IllegalArgumentException("Idade mínima 18 anos.");

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            if (!FieldValidation.isValidPassword(dto.getSenha())) {
                throw new IllegalArgumentException("Senha inválida.");
            }
        }
    }

}