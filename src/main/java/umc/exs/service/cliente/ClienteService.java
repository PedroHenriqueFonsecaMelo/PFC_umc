package umc.exs.service.cliente;

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
import umc.exs.dto.request.cliente.ClienteUpdateRequest;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.model.entidades.foundation.EmailVerificacao;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.foundation.EmailVerificacaoRepository;
import umc.exs.service.carteira.CarteiraService;
import umc.exs.service.cliente.delegado.ClienteAutenticacaoService;
import umc.exs.service.cliente.delegado.ClientePerfilService;
import umc.exs.service.cliente.delegado.ClienteRepositoryService;
import umc.exs.service.cliente.senha.FieldValidation;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

        private final ClienteRepositoryService repositoryService;
        private final ClientePerfilService perfilService;
        private final ClienteAutenticacaoService autenticacaoService;
        private final CarteiraService carteiraService;

        private final LogAuditoriaService auditoria;
        private final PasswordEncoder passwordEncoder;
        private final EmailVerificacaoRepository emailVerificacaoRepository;
        private final EmailFacade emailFacade;

        private final SecureRandom secureRandom = new SecureRandom();

        @Value("${app.base-url:http://localhost:8443}")
        private String baseUrl;

        @Transactional
        public Cliente salvarCliente(SignupRequest signupRequest) {
                validarNovoCliente(signupRequest);

                Cliente cliente = perfilService.cadastrar(signupRequest);

                auditoria.registrarLog(
                                "CADASTRO_USUARIO",
                                "Cadastro inicial realizado.");

                enviarEmailVerificacao(
                                cliente.getId(),
                                cliente.getNome(),
                                cliente.getEmail());

                return cliente;
        }

        @Transactional
        public Cliente salvarClienteCompleto(
                        SignupRequest signupRequest,
                        Endereco endereco) {

                validarNovoCliente(signupRequest);

                Cliente cliente = perfilService.cadastrarCompleto(
                                signupRequest,
                                endereco);

                auditoria.registrarLog(
                                "CADASTRO_COMPLETO",
                                "Cadastro completo realizado.");

                return cliente;
        }

        private void enviarEmailVerificacao(
                        @NonNull Long clienteId,
                        String nome,
                        String email) {

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

                        emailFacade.sendHtmlSafe(
                                        email,
                                        "Confirme seu e-mail — Bibliotroca",
                                        EmailHtmlBuilder.verificacaoEmail(nome, link));

                } catch (Exception e) {

                        log.error(
                                        "Falha ao enviar e-mail de verificação para {}: {}",
                                        email,
                                        e.getMessage());
                }
        }

        @Transactional
        public String uploadFotoPerfil(
                        @NonNull Long clienteId,
                        MultipartFile foto) {

                return perfilService.atualizarFoto(
                                clienteId,
                                foto);
        }

        @Transactional
        public void uploadFotoPerfilParaUsuarioLogado(
                        String email,
                        MultipartFile foto) {

                Cliente cliente = buscarEntidadePorEmail(email);

                perfilService.atualizarFoto(
                                cliente.getId(),
                                foto);
        }

        @Transactional
        public void atualizarDadosLogados(
                        String email,
                        ClienteUpdateRequest dto) {

                Cliente cliente = buscarEntidadePorEmail(email);

                perfilService.atualizarDados(
                                cliente.getId(),
                                dto);
        }

        @Transactional
        public void adicionarTokensParaUsuarioLogado(
                        String email,
                        Double valor) {

                Cliente cliente = buscarEntidadePorEmail(email);

                carteiraService.adicionarTokens(
                                cliente,
                                valor,
                                "PIX",
                                "Recarga manual");
        }

        @Transactional
        public Cliente adicionarTokens(
                        Long clienteId,
                        Double valor) {

                Cliente cliente = repositoryService.buscarPorId(clienteId);

                carteiraService.adicionarTokens(
                                cliente,
                                valor,
                                "PIX",
                                "Recarga PIX");

                return cliente;
        }

        public Cliente buscarEntidadePorEmail(String email) {
                return repositoryService.buscarPorEmailOuFalhar(email);
        }

        @Transactional
        public void deletarContaPropria(String email) {

                Cliente cliente = buscarEntidadePorEmail(email);

                Long id = cliente.getId();

                String charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%*.";

                String senhaAleatoria = secureRandom
                                .ints(50, 0, charPool.length())
                                .mapToObj(i -> String.valueOf(charPool.charAt(i)))
                                .collect(Collectors.joining());

                cliente.setEmail(
                                passwordEncoder.encode(
                                                "anonimo_" + id + "@exs.com.br"));

                cliente.setNome("Usuário Excluído");
                cliente.setCpf("000.000.000-00");
                cliente.setFotoPerfil(null);

                cliente.setSenha(
                                passwordEncoder.encode(senhaAleatoria));

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

        public Optional<Cliente> buscarClientePorEmail(
                        String email) {

                return repositoryService.encontrarPorEmail(email);
        }

        public Cliente buscarPorId(
                        @NonNull Long id) {

                return repositoryService.buscarPorId(id);
        }

        public void validarNovoCliente(SignupRequest dto) {

                if (!Boolean.TRUE.equals(dto.getTermsAccepted())) {
                        throw new IllegalArgumentException("Aceite os termos.");
                }

                if (!Boolean.TRUE.equals(dto.getPrivacyAccepted())) {
                        throw new IllegalArgumentException("Aceite a política.");
                }

                String email = FieldValidation.sanitizeEmail(dto.getEmail());

                if (repositoryService.existeEmailAtivo(email)) {
                        throw new IllegalArgumentException("Email já cadastrado.");
                }

                dto.setEmail(email);
        }

        public void validarAtualizacao(
                        String nome,
                        String senha) {

                if (nome == null || nome.trim().isEmpty()) {
                        throw new IllegalArgumentException("Nome obrigatório.");
                }

                if (senha != null
                                && !senha.trim().isEmpty()
                                && !FieldValidation.isValidPassword(senha)) {

                        throw new IllegalArgumentException("Senha inválida.");
                }
        }

        @Transactional
        public void registrarTransacaoPendente(
                        @NonNull Long clienteId,
                        double valor,
                        String pagamentoId) {

                Cliente cliente = repositoryService.buscarPorId(clienteId);

                carteiraService.registrarIntencaoPagamento(
                                cliente,
                                valor,
                                pagamentoId);
        }

        @Transactional(readOnly = true)
        public List<Transacao> listarHistoricoTransacoes(
                        String email) {

                Cliente cliente = buscarEntidadePorEmail(email);

                return carteiraService.listarHistoricoPorCliente(
                                cliente.getId());
        }

        @Transactional(readOnly = true)
        public List<Transacao> listarHistoricoTransacoes(
                        @NonNull Long id) {

                return carteiraService.listarHistoricoPorCliente(id);
        }

        public boolean verificarSeFoiPago(
                        String pagamentoId) {

                return carteiraService.verificarStatusPagamento(
                                pagamentoId);
        }

        public void aprovarPagamento(
                        String pagamentoId) {

                carteiraService.confirmarPagamentoPix(
                                pagamentoId);
        }

        @Transactional
        public Cliente autenticarCliente(
                        String email,
                        String senha) {

                Cliente cliente = autenticacaoService.autenticar(
                                email,
                                senha);

                auditoria.registrarLog(
                                "LOGIN_SUCESSO",
                                "Sessão iniciada.");

                return cliente;
        }

        @Transactional
        public void iniciarRecuperacaoSenha(
                        String email) {

                Cliente cliente = buscarEntidadePorEmail(email);

                autenticacaoService.gerarTokenRecuperacao(cliente);
        }

        public boolean validarTokenRecuperacao(
                        String token) {

                return autenticacaoService.validarToken(token);
        }

        @Transactional
        public void alterarSenhaLogado(
                        String email,
                        String senhaAtual,
                        String novaSenha,
                        String confirmarSenha) {

                if (!novaSenha.equals(confirmarSenha)) {
                        throw new IllegalArgumentException(
                                        "As novas senhas não conferem.");
                }

                if (!FieldValidation.isValidPassword(novaSenha)) {
                        throw new IllegalArgumentException(
                                        "A nova senha não atende aos requisitos.");
                }

                autenticacaoService.alterarSenha(
                                email,
                                senhaAtual,
                                novaSenha);

                auditoria.registrarLog(
                                "ALTERACAO_SENHA",
                                "Senha alterada.");
        }

        @Transactional
        public void alterarSenhaComToken(
                        String token,
                        String novaSenha) {

                if (!FieldValidation.isValidPassword(novaSenha)) {
                        throw new IllegalArgumentException(
                                        "Senha inválida.");
                }

                autenticacaoService.redefinirSenha(
                                token,
                                novaSenha);
        }

        @Transactional
        public void adicionarEnderecoParaUsuarioLogado(
                        String email,
                        Endereco endereco) {

                repositoryService.adicionarEnderecoParaUsuarioLogado(
                                email,
                                endereco);
        }

        @Transactional
        public void selecionarEnderecoParaUsuarioLogado(
                        String email,
                        Long enderecoId) {

                Cliente cliente = buscarEntidadePorEmail(email);

                boolean pertence = cliente.getEnderecos()
                                .stream()
                                .anyMatch(e -> e.getId().equals(enderecoId));

                if (!pertence) {
                        throw new IllegalArgumentException(
                                        "Endereço não pertence a este cliente.");
                }

                cliente.setEnderecoSelecionadoId(enderecoId);

                repositoryService.salvar(cliente);
        }

        @Transactional
        public void atualizarEnderecoDoCliente(
                        @NonNull Long clienteId,
                        @NonNull Endereco dto) {

                repositoryService.atualizarEnderecoDoCliente(
                                clienteId,
                                dto);
        }

        @Transactional
        public void deletarEnderecoDoCliente(
                        @NonNull Long clienteId,
                        @NonNull Long enderecoId) {

                repositoryService.deletarEnderecoDoCliente(
                                clienteId,
                                enderecoId);
        }

        @Transactional
        public void deletarCartaoDoCliente(
                        @NonNull Long clienteId,
                        @NonNull Long cartaoId) {

                repositoryService.deletarCartaoDoCliente(
                                clienteId,
                                cartaoId);
        }

}
