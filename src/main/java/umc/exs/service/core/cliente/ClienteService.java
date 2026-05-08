package umc.exs.service.core.cliente;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.CartaoDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.mappers.ClienteMapper;
import umc.exs.model.entidades.foundation.EmailVerificacao;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.foundation.EmailVerificacaoRepository;
import umc.exs.service.email.EmailService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.senha.FieldValidation;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteDomainService domainService;
    private final ClienteRepositoryService repositoryService;
    private final LogAuditoriaService auditoria;

    private final PasswordEncoder passwordEncoder;
    private final ClienteMapper clienteMapper;
    private final EmailVerificacaoRepository emailVerificacaoRepository;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        validarNovoCliente(signupDTO);
        ClienteDTO dto = domainService.cadastrarCliente(signupDTO);
        auditoria.registrarLog("CADASTRO_USUARIO", dto.getId(), dto.getEmail(), "Cadastro inicial realizado.");
        enviarEmailVerificacao(dto.getId(), dto.getNome(), dto.getEmail());
        return dto;
    }

    private void enviarEmailVerificacao(Long clienteId, String nome, String email) {
        try {
            // Remove token anterior se existir
            emailVerificacaoRepository.deleteByClienteId(clienteId);

            Cliente cliente = repositoryService.buscarPorId(clienteId);
            String token = UUID.randomUUID().toString();

            EmailVerificacao verificacao = EmailVerificacao.builder()
                    .cliente(cliente)
                    .token(token)
                    .expiracao(LocalDateTime.now().plusHours(24))
                    .build();
            emailVerificacaoRepository.save(verificacao);

            String link = baseUrl + "/auth/verificar-email?token=" + token;
            emailService.enviar(
                    email,
                    "Confirme seu e-mail — Bibliotroca",
                    "Olá, " + nome + "!\n\n" +
                            "Obrigado por se cadastrar. Clique no link abaixo para confirmar seu e-mail:\n\n" +
                            link + "\n\n" +
                            "O link expira em 24 horas.\n\n" +
                            "Equipe Bibliotroca");
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de verificação para {}: {}", email, e.getMessage());
        }
    }

    @Transactional
    public ClienteDTO salvarClienteCompleto(SignupDTO signupDTO, EnderecoDTO enderecoDTO, CartaoDTO cartaoDTO) {
        validarNovoCliente(signupDTO);
        ClienteDTO dto = domainService.cadastrarClienteCompleto(signupDTO, enderecoDTO, cartaoDTO);
        auditoria.registrarLog("CADASTRO_COMPLETO", dto.getId(), dto.getEmail(), "Cadastro completo realizado.");
        return dto;
    }

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO dto) {
        validarAtualizacao(dto.getNome(), dto.getSenha());
        ClienteDTO atualizado = domainService.atualizarDados(clienteId, dto);
        auditoria.registrarLog("ATUALIZACAO_DADOS", clienteId, atualizado.getEmail(), "Dados atualizados.");
        return atualizado;
    }

    @Transactional
    public String uploadFotoPerfil(@NonNull Long clienteId, MultipartFile foto) {
        String url = domainService.gerenciarUploadFoto(clienteId, foto);
        Cliente c = repositoryService.buscarPorId(clienteId);
        auditoria.registrarLog("UPLOAD_FOTO", clienteId, c.getEmail(), "Foto de perfil atualizada.");
        return url;
    }

    @Transactional
    public void deletarClientePorId(@NonNull Long clienteId) {
        Cliente cliente = repositoryService.buscarPorId(clienteId);
        String email = cliente.getEmail();
        // Soft delete — dados financeiros são preservados (LGPD Art. 16)
        repositoryService.deletarPorId(clienteId);
        auditoria.registrarLog("EXCLUSAO_CONTA", clienteId, email, "Conta marcada como inativa (soft delete).");
    }

    @Transactional
    public void uploadFotoPerfilParaUsuarioLogado(String email, MultipartFile foto) {
        Cliente cliente = buscarEntidadePorEmail(email);
        this.uploadFotoPerfil(cliente.getId(), foto);
    }

    @Transactional
    public void atualizarDadosLogados(String email, ClienteDTO dto) {
        Cliente cliente = buscarEntidadePorEmail(email);
        this.atualizarClienteEAssociacoes(cliente.getId(), dto);
    }

    @Transactional
    public void deletarContaPropria(String email) {

        Cliente cliente = buscarEntidadePorEmail(email);
        Long id = cliente.getId();

        String charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%*.";
        String senhaAleatoria = new SecureRandom().ints(50, 0, charPool.length())
                .mapToObj(i -> String.valueOf(charPool.charAt(i)))
                .collect(Collectors.joining());

        cliente.setEmail(passwordEncoder.encode("anonimo_" + id + "@exs.com.br"));
        cliente.setNome("Usuário Excluído");
        cliente.setCpf("000.000.000-00");
        cliente.setFotoPerfil(null);
        cliente.setSaldoTokens(0.0);
        cliente.setSenha(passwordEncoder.encode(senhaAleatoria));
        cliente.setTentativas(10);
        cliente.setBloqueada(true);
        // Soft delete com anonimização — dados financeiros são preservados (LGPD Art. 16)
        cliente.setAtivo(false);
        cliente.setDeletedAt(LocalDateTime.now());

        if (cliente.getCartoes() != null) {
            cliente.getCartoes().clear();
        }
        if (cliente.getEnderecos() != null) {
            cliente.getEnderecos().clear();
        }

        repositoryService.salvar(cliente);
        log.info("Conta do cliente ID {} anonimizada com sucesso por solicitação do usuário.", id);
    }

    /**
     * Autentica o cliente. Lança {@link IllegalArgumentException} com mensagem
     * específica em caso de falha (e-mail não encontrado, senha errada, bloqueado).
     */
    @Transactional
    public ClienteDTO autenticarCliente(String email, String senha) {
        ClienteDTO resultado = domainService.processarAutenticacao(email, senha);
        auditoria.registrarLog("LOGIN_SUCESSO", resultado.getId(), resultado.getEmail(), "Sessão iniciada.");
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        return repositoryService.encontrarPorEmail(email).map(domainService::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidadePorEmail(String email) {
        return repositoryService.buscarPorEmailOuFalhar(email);
    }

    @Transactional
    public void iniciarRecuperacaoSenha(String email) {
        Cliente cliente = buscarEntidadePorEmail(email);
        domainService.gerarTokenRecuperacao(cliente);
    }

    @Transactional
    public void alterarSenhaComToken(String token, String novaSenha) {
        if (!FieldValidation.isValidPassword(novaSenha)) {
            throw new IllegalArgumentException("Senha não atende aos requisitos.");
        }

        var cliente = domainService.redefinirSenha(token, novaSenha);

        log.debug("Iniciando envio de e-mail de confirmação de redefinição para: {}", cliente.getEmail());
        String dataHora = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        try {
            emailService.enviar(
                    cliente.getEmail(),
                    "Sua senha foi redefinida — Bibliotroca",
                    "Olá, " + cliente.getNome() + "!\n\n" +
                    "Sua senha foi redefinida com sucesso em " + dataHora + ".\n\n" +
                    "Se você realizou essa alteração, pode ignorar este e-mail.\n\n" +
                    "Se você não reconhece essa atividade, sua conta pode estar comprometida. " +
                    "Entre em contato com nossa equipe imediatamente pelo e-mail " +
                    "bibliotroca.noreply@gmail.com para que possamos proteger sua conta.\n\n" +
                    "Equipe Bibliotroca");
            log.info("E-mail de confirmação de redefinição enviado para: {}", cliente.getEmail());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de confirmação de redefinição para {}: {}",
                    cliente.getEmail(), e.getMessage());
        }
    }

    public ClienteDTO buscarPorId(@NonNull Long id) {
        return domainService.converterParaDTO(repositoryService.buscarPorId(id));
    }

    @Transactional
    public ClienteDTO adicionarTokens(Long clienteId, Double valor) {
        ClienteDTO dto = domainService.adicionarTokens(clienteId, valor);
        auditoria.registrarLog("RECARGA_TOKENS", clienteId, dto.getEmail(),
                String.format("Recarga de %.2f via PIX", valor));
        return dto;
    }

    @Transactional
    public void adicionarTokensParaUsuarioLogado(String email, Double valor) {
        Cliente cliente = buscarEntidadePorEmail(email);
        this.adicionarTokens(cliente.getId(), valor);
    }

    @Transactional
    public void adicionarEnderecoParaUsuarioLogado(String email, EnderecoDTO enderecoDTO) {
        repositoryService.adicionarEnderecoParaUsuarioLogado(email, enderecoDTO);
    }

    public void aprovarPagamento(String pagamentoId) {
        domainService.aprovarPagamento(pagamentoId);
    }

    public boolean verificarSeFoiPago(String pagamentoId) {
        return domainService.verificarSeFoiPago(pagamentoId);
    }

    public void registrarTransacaoPendente(@NonNull Long clienteId, Double valor, String pagamentoId) {
        Cliente cliente = repositoryService.buscarPorId(clienteId);
        domainService.registrarTransacaoPendente(cliente, valor, pagamentoId);
    }

    public List<Transacao> listarHistoricoTransacoes(String email) {
        Cliente cliente = buscarEntidadePorEmail(email);
        return domainService.listarHistoricoTransacoes(cliente);
    }

    public List<Transacao> listarHistoricoTransacoes(@NonNull Long id) {
        Cliente cliente = repositoryService.buscarPorId(id);
        return domainService.listarHistoricoTransacoes(cliente);
    }

    public void validarNovoCliente(SignupDTO dto) {
        if (!Boolean.TRUE.equals(dto.getTermsAccepted()))
            throw new IllegalArgumentException("Você deve aceitar os Termos de Uso para se cadastrar.");
        if (!Boolean.TRUE.equals(dto.getPrivacyAccepted()))
            throw new IllegalArgumentException("Você deve aceitar a Política de Privacidade para se cadastrar.");

        String safeEmail = FieldValidation.sanitizeEmail(dto.getEmail());
        if (repositoryService.existeEmailAtivo(safeEmail))
            throw new IllegalArgumentException("E-mail já cadastrado.");
        dto.setEmail(safeEmail);

        // Validação de CPF (dígitos verificadores + unicidade entre contas ativas)
        if (dto.getCpf() != null && !dto.getCpf().trim().isEmpty()) {
            if (!FieldValidation.isValidCPF(dto.getCpf()))
                throw new IllegalArgumentException("CPF inválido.");
            if (repositoryService.existeCpfAtivo(dto.getCpf()))
                throw new IllegalArgumentException("CPF já cadastrado para outro usuário ativo.");
        }

        LocalDate dataNasc = FieldValidation.isValidBirthDate(dto.getDatanasc());
        if (dataNasc == null || !FieldValidation.isOver18(dataNasc))
            throw new IllegalArgumentException("Cliente deve ser maior de 18 anos.");

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()
                && !FieldValidation.isValidPassword(dto.getSenha())) {
            throw new IllegalArgumentException("Senha não atende aos requisitos de segurança.");
        }
    }

    public void validarAtualizacao(String nome, String senha) {
        if (nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("Nome é obrigatório.");
        if (senha != null && !senha.trim().isEmpty() && !FieldValidation.isValidPassword(senha)) {
            throw new IllegalArgumentException("Nova senha inválida.");
        }
    }

    @Transactional
    public void alterarSenhaLogado(String email, String senhaAtual, String novaSenha, String confirmarSenha) {
        if (!novaSenha.equals(confirmarSenha)) {
            throw new IllegalArgumentException("As novas senhas não conferem.");
        }
        if (!FieldValidation.isValidPassword(novaSenha)) {
            throw new IllegalArgumentException("A nova senha não atende aos requisitos de segurança.");
        }
        domainService.alterarSenhaComVerificacao(email, senhaAtual, novaSenha);
        auditoria.registrarLog("ALTERACAO_SENHA", null, email, "Senha alterada pelo usuário logado.");
    }

    public boolean validarTokenRecuperacao(String token) {
        return domainService.validarToken(token);
    }

    public void deletarEnderecoDoCliente(@NonNull Long clienteId, @NonNull Long enderecoId) {
        repositoryService.deletarEnderecoDoCliente(clienteId, enderecoId);
    }

    @Transactional
    public void atualizarEnderecoDoCliente(@NonNull Long clienteId, EnderecoDTO dto) {
        repositoryService.atualizarEnderecoDoCliente(clienteId, dto);
    }

    public void deletarCartaoDoCliente(@NonNull Long clienteId, @NonNull Long enderecoId) {
        repositoryService.deletarCartaoDoCliente(clienteId, enderecoId);
    }
}