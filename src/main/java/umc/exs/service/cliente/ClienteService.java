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

/**
 * Serviço principal de cliente — orquestra cadastro, autenticação, perfil, carteira e endereços.
 * Delega responsabilidades específicas para serviços auxiliares (delegados).
 */
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

        /**
         * Cadastra um novo cliente, registra log de auditoria e envia e-mail de verificação.
         * Lança exceção se o e-mail já estiver em uso ou os termos não forem aceitos.
         */
        @Transactional
        public Cliente salvarCliente(SignupRequest signupRequest) {
                validarNovoCliente(signupRequest);

                Cliente cliente = perfilService.cadastrar(signupRequest);

                auditoria.registrarLog(
                                "CADASTRO_USUARIO",
                                cliente.getId(),
                                cliente.getEmail(),
                                "Cadastro inicial realizado.");

                enviarEmailVerificacao(
                                cliente.getId(),
                                cliente.getNome(),
                                cliente.getEmail());

                return cliente;
        }

        /**
         * Cadastra cliente com endereço completo em um único passo transacional.
         * Registra log de auditoria com ação CADASTRO_COMPLETO.
         */
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
                                cliente.getId(),
                                cliente.getEmail(),
                                "Cadastro completo realizado.");

                return cliente;
        }

        /**
         * Gera e envia um link de verificação de e-mail com validade de 24 horas.
         * Remove token anterior antes de criar um novo para evitar duplicidade.
         */
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

        /** Faz upload e atualiza a foto de perfil do cliente identificado pelo ID. */
        @Transactional
        public String uploadFotoPerfil(
                        @NonNull Long clienteId,
                        MultipartFile foto) {

                return perfilService.atualizarFoto(
                                clienteId,
                                foto);
        }

        /** Faz upload da foto de perfil para o cliente identificado pelo e-mail da sessão. */
        @Transactional
        public void uploadFotoPerfilParaUsuarioLogado(
                        String email,
                        MultipartFile foto) {

                Cliente cliente = buscarEntidadePorEmail(email);

                perfilService.atualizarFoto(
                                cliente.getId(),
                                foto);
        }

        /** Atualiza os dados cadastrais do cliente identificado pelo e-mail da sessão. */
        @Transactional
        public void atualizarDadosLogados(
                        String email,
                        ClienteUpdateRequest dto) {

                Cliente cliente = buscarEntidadePorEmail(email);

                perfilService.atualizarDados(
                                cliente.getId(),
                                dto);
        }

        /** Adiciona tokens à carteira do cliente logado via recarga manual (PIX). */
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

        /** Adiciona tokens à carteira do cliente identificado pelo ID via recarga PIX. */
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

        /** Busca o cliente pelo e-mail ou lança exceção se não encontrado. */
        public Cliente buscarEntidadePorEmail(String email) {
                return repositoryService.buscarPorEmailOuFalhar(email);
        }

        /**
         * Anonimiza os dados pessoais do cliente, bloqueia a conta e marca como removida.
         * O e-mail é substituído por hash e a senha por valor aleatório irrecuperável.
         */
        @Transactional
        public void deletarContaPropria(String email) {

                Cliente cliente = buscarEntidadePorEmail(email);

                Long id = cliente.getId();

                // ANONIMIZAÇÃO — dados pessoais substituídos para garantir privacidade após exclusão
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

                // BLOQUEIO PERMANENTE — conta desativada e bloqueada para impedir reuso após exclusão
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

        /** Busca o cliente pelo e-mail retornando Optional; sem lançar exceção se não encontrado. */
        public Optional<Cliente> buscarClientePorEmail(
                        String email) {

                return repositoryService.encontrarPorEmail(email);
        }

        /** Busca o cliente pelo ID ou lança exceção se não existir. */
        public Cliente buscarPorId(
                        @NonNull Long id) {

                return repositoryService.buscarPorId(id);
        }

        /**
         * Valida os dados do novo cliente: aceite de termos, e-mail único e formatação válida.
         * Lança IllegalArgumentException com mensagem específica para cada violação.
         */
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

        /** Registra uma intenção de pagamento pendente (ex.: PIX gerado mas ainda não confirmado). */
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

        /** Lista o histórico de transações da carteira do cliente identificado pelo e-mail. */
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

        /** Verifica se um pagamento PIX já foi confirmado consultando o status pelo ID. */
        public boolean verificarSeFoiPago(
                        String pagamentoId) {

                return carteiraService.verificarStatusPagamento(
                                pagamentoId);
        }

        /** Confirma o pagamento PIX e credita o valor na carteira do cliente. */
        public void aprovarPagamento(
                        String pagamentoId) {

                carteiraService.confirmarPagamentoPix(
                                pagamentoId);
        }

        /**
         * Autentica o cliente delegando ao serviço de autenticação e registra log de sucesso.
         * Lança exceção em caso de credenciais inválidas, conta bloqueada ou suspensa.
         */
        @Transactional
        public Cliente autenticarCliente(
                        String email,
                        String senha) {

                Cliente cliente = autenticacaoService.autenticar(
                                email,
                                senha);

                auditoria.registrarLog(
                                "LOGIN_SUCESSO",
                                cliente.getId(),
                                cliente.getEmail(),
                                "Sessão iniciada.");

                return cliente;
        }

        /** Inicia o processo de recuperação de senha gerando e enviando token por e-mail. */
        @Transactional
        public void iniciarRecuperacaoSenha(
                        String email) {

                Cliente cliente = buscarEntidadePorEmail(email);

                autenticacaoService.gerarTokenRecuperacao(cliente);
        }

        /** Verifica se o token de recuperação de senha é válido e ainda não expirou. */
        public boolean validarTokenRecuperacao(
                        String token) {

                return autenticacaoService.validarToken(token);
        }

        /**
         * Altera a senha do cliente logado após confirmar que nova senha e confirmação coincidem.
         * Valida a força da nova senha antes de prosseguir.
         */
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
                                null,
                                email,
                                "Senha alterada.");
        }

        /** Redefine a senha usando o token de recuperação recebido por e-mail após validar a força. */
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

        /** Adiciona um novo endereço à lista de endereços do cliente logado. */
        @Transactional
        public void adicionarEnderecoParaUsuarioLogado(
                        String email,
                        Endereco endereco) {

                repositoryService.adicionarEnderecoParaUsuarioLogado(
                                email,
                                endereco);
        }

        /**
         * Define o endereço de entrega principal do cliente após verificar que o endereço pertence a ele.
         * Lança exceção se o endereço informado não for do cliente logado.
         */
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

        /** Atualiza os dados de um endereço existente do cliente pelo ID. */
        @Transactional
        public void atualizarEnderecoDoCliente(
                        @NonNull Long clienteId,
                        @NonNull Endereco dto) {

                repositoryService.atualizarEnderecoDoCliente(
                                clienteId,
                                dto);
        }

        /** Remove o endereço informado da lista de endereços do cliente. */
        @Transactional
        public void deletarEnderecoDoCliente(
                        @NonNull Long clienteId,
                        @NonNull Long enderecoId) {

                repositoryService.deletarEnderecoDoCliente(
                                clienteId,
                                enderecoId);
        }

        /** Remove o cartão de pagamento informado da lista de cartões do cliente. */
        @Transactional
        public void deletarCartaoDoCliente(
                        @NonNull Long clienteId,
                        @NonNull Long cartaoId) {

                repositoryService.deletarCartaoDoCliente(
                                clienteId,
                                cartaoId);
        }

}
