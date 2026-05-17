package umc.exs.service.core.cliente;

import java.security.SecureRandom;
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
import umc.exs.dto.auth.SignupDTO;
import umc.exs.dto.user.CartaoDTO;
import umc.exs.dto.user.ClienteDTO;
import umc.exs.dto.user.ClienteUpdateDTO;
import umc.exs.dto.user.EnderecoDTO;
import umc.exs.model.entidades.foundation.EmailVerificacao;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.foundation.EmailVerificacaoRepository;
import umc.exs.service.email.EmailHtmlBuilder;
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
    private final EmailVerificacaoRepository emailVerificacaoRepository;
    private final EmailService emailService;

    private final ClienteOperacaoService operacaoService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:8443}")
    private String baseUrl;

    @Transactional
    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        validarNovoCliente(signupDTO);
        ClienteDTO dto = domainService.cadastrarCliente(signupDTO);
        auditoria.registrarLog("CADASTRO_USUARIO", dto.getId(), dto.getEmail(), "Cadastro inicial realizado.");
        enviarEmailVerificacao(dto.getId(), dto.getNome(), dto.getEmail());
        return dto;
    }

    @SuppressWarnings("null")
    private void enviarEmailVerificacao(@NonNull Long clienteId, String nome, String email) {
        try {
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

            emailService.enviarHtml(
                    email,
                    "Confirme seu e-mail — Bibliotroca",
                    EmailHtmlBuilder.verificacaoEmail(nome, link));

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
    public String uploadFotoPerfil(@NonNull Long clienteId, MultipartFile foto) {
        String url = domainService.gerenciarUploadFoto(clienteId, foto);
        Cliente c = repositoryService.buscarPorId(clienteId);
        auditoria.registrarLog("UPLOAD_FOTO", clienteId, c.getEmail(), "Foto atualizada.");
        return url;
    }

    @Transactional
    public void uploadFotoPerfilParaUsuarioLogado(String email, MultipartFile foto) {
        Cliente cliente = buscarEntidadePorEmail(email);
        operacaoService.uploadFotoPerfil(cliente.getId(), foto);
    }

    @Transactional
    public void atualizarDadosLogados(String email, ClienteUpdateDTO dto) {
        Cliente cliente = buscarEntidadePorEmail(email);
        operacaoService.atualizarCliente(cliente.getId(), dto);
    }

    @Transactional
    public void adicionarTokensParaUsuarioLogado(String email, Double valor) {
        Cliente cliente = buscarEntidadePorEmail(email);
        operacaoService.adicionarTokens(cliente.getId(), valor);
    }

    @Transactional
    public ClienteDTO adicionarTokens(Long clienteId, Double valor) {
        ClienteDTO dto = domainService.adicionarTokens(clienteId, valor);
        auditoria.registrarLog("RECARGA_TOKENS", clienteId, dto.getEmail(),
                String.format("Recarga de %.2f via PIX", valor));
        return dto;
    }

    public Cliente buscarEntidadePorEmail(String email) {
        return repositoryService.buscarPorEmailOuFalhar(email);
    }

    @Transactional
    public void deletarContaPropria(String email) {

        Cliente cliente = buscarEntidadePorEmail(email);
        Long id = cliente.getId();

        String charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%*.";

        String senhaAleatoria = secureRandom.ints(50, 0, charPool.length())
                .mapToObj(i -> String.valueOf(charPool.charAt(i)))
                .collect(Collectors.joining());

        cliente.setEmail(passwordEncoder.encode("anonimo_" + id + "@exs.com.br"));
        cliente.setNome("Usuário Excluído");
        cliente.setCpf("000.000.000-00");
        cliente.setFotoPerfil(null);

        cliente.setSenha(passwordEncoder.encode(senhaAleatoria));
        cliente.setTentativas(10);
        cliente.setBloqueada(true);
        cliente.setAtivo(false);
        cliente.setDeletedAt(LocalDateTime.now());

        if (cliente.getCartoes() != null) {
            cliente.getCartoes().clear();
        }
        if (cliente.getEnderecos() != null) {
            cliente.getEnderecos().clear();
        }

        repositoryService.salvar(cliente);

        log.info("Conta {} anonimizada.", id);
    }

    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        return repositoryService.encontrarPorEmail(email)
                .map(domainService::converterParaDTO);
    }

    public ClienteDTO buscarPorId(@NonNull Long id) {
        return domainService.converterParaDTO(repositoryService.buscarPorId(id));
    }

    public void validarNovoCliente(SignupDTO dto) {

        if (!Boolean.TRUE.equals(dto.getTermsAccepted()))
            throw new IllegalArgumentException("Aceite os termos.");

        if (!Boolean.TRUE.equals(dto.getPrivacyAccepted()))
            throw new IllegalArgumentException("Aceite a política.");

        String email = FieldValidation.sanitizeEmail(dto.getEmail());

        if (repositoryService.existeEmailAtivo(email))
            throw new IllegalArgumentException("Email já cadastrado.");

        dto.setEmail(email);
    }

    public void validarAtualizacao(String nome, String senha) {

        if (nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("Nome obrigatório.");

        if (senha != null && !senha.trim().isEmpty()
                && !FieldValidation.isValidPassword(senha)) {

            throw new IllegalArgumentException("Senha inválida.");
        }
    }

    @Transactional
    public void registrarTransacaoPendente(@NonNull Long clienteId, double valor, String pagamentoId) {
        Cliente cliente = repositoryService.buscarPorId(clienteId);
        domainService.registrarTransacaoPendente(cliente, valor, pagamentoId);
    }

    @Transactional(readOnly = true)
    public List<Transacao> listarHistoricoTransacoes(String email) {
        Cliente cliente = buscarEntidadePorEmail(email);
        return domainService.listarHistoricoTransacoes(cliente);
    }

    @Transactional(readOnly = true)
    public List<Transacao> listarHistoricoTransacoes(@NonNull Long id) {
        Cliente cliente = repositoryService.buscarPorId(id);
        return domainService.listarHistoricoTransacoes(cliente);
    }

    public boolean verificarSeFoiPago(String pagamentoId) {
        return domainService.verificarSeFoiPago(pagamentoId);
    }

    public void aprovarPagamento(String pagamentoId) {
        domainService.aprovarPagamento(pagamentoId);
    }

    @Transactional
    public ClienteDTO autenticarCliente(String email, String senha) {
        ClienteDTO resultado = domainService.processarAutenticacao(email, senha);
        auditoria.registrarLog("LOGIN_SUCESSO", resultado.getId(), resultado.getEmail(), "Sessão iniciada.");
        return resultado;
    }

    @Transactional
    public void iniciarRecuperacaoSenha(String email) {
        Cliente cliente = buscarEntidadePorEmail(email);
        domainService.gerarTokenRecuperacao(cliente);
    }

    public boolean validarTokenRecuperacao(String token) {
        return domainService.validarToken(token);
    }

    @Transactional
    public void alterarSenhaLogado(String email, String senhaAtual, String novaSenha, String confirmarSenha) {
        if (!novaSenha.equals(confirmarSenha)) {
            throw new IllegalArgumentException("As novas senhas não conferem.");
        }
        if (!FieldValidation.isValidPassword(novaSenha)) {
            throw new IllegalArgumentException("A nova senha não atende aos requisitos.");
        }
        domainService.alterarSenhaComVerificacao(email, senhaAtual, novaSenha);
        auditoria.registrarLog("ALTERACAO_SENHA", null, email, "Senha alterada.");
    }

    @Transactional
    public void alterarSenhaComToken(String token, String novaSenha) {
        if (!FieldValidation.isValidPassword(novaSenha)) {
            throw new IllegalArgumentException("Senha inválida.");
        }
        domainService.redefinirSenha(token, novaSenha);
    }

    @Transactional
    public void adicionarEnderecoParaUsuarioLogado(String email, EnderecoDTO enderecoDTO) {
        repositoryService.adicionarEnderecoParaUsuarioLogado(email, enderecoDTO);
    }

    @Transactional
    public void selecionarEnderecoParaUsuarioLogado(String email, Long enderecoId) {
        Cliente cliente = buscarEntidadePorEmail(email);
        boolean pertence = cliente.getEnderecos().stream().anyMatch(e -> e.getId().equals(enderecoId));
        if (!pertence) {
            throw new IllegalArgumentException("Endereço não pertence a este cliente.");
        }
        cliente.setEnderecoSelecionadoId(enderecoId);
        repositoryService.salvar(cliente);
    }

    @Transactional
    public void atualizarEnderecoDoCliente(@NonNull Long clienteId, @NonNull EnderecoDTO dto) {
        repositoryService.atualizarEnderecoDoCliente(clienteId, dto);
    }

    @Transactional
    public void deletarEnderecoDoCliente(@NonNull Long clienteId, @NonNull Long enderecoId) {
        repositoryService.deletarEnderecoDoCliente(clienteId, enderecoId);
    }

    @Transactional
    public void deletarCartaoDoCliente(@NonNull Long clienteId, @NonNull Long cartaoId) {
        repositoryService.deletarCartaoDoCliente(clienteId, cartaoId);
    }
}