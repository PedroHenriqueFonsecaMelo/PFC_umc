package umc.exs.service.cliente.delegado;

import java.time.format.DateTimeFormatter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.StatusConta;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;
import umc.exs.service.cliente.senha.SenhaService;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;

/**
 * Serviço delegado responsável pela autenticação, bloqueio de conta e recuperação de senha.
 * Controla tentativas incorretas de login e garante a segurança do acesso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteAutenticacaoService {

        private final ClienteRepositoryService repositoryService;
        private final PasswordEncoder passwordEncoder;
        private final RecuperacaoSenhaRepository tokenRepository;
        private final SenhaService senhaService;
        private final LogAuditoriaService logAuditoria;

        /**
         * Autentica o cliente verificando status da conta, e-mail verificado e senha.
         * Incrementa tentativas falhas e bloqueia a conta após o limite ser atingido.
         */
        @Transactional
        public Cliente autenticar(String email, String senha) {

                Cliente cliente = repositoryService.encontrarPorEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

                StatusConta status = cliente.getStatusConta() != null ? cliente.getStatusConta() : StatusConta.ATIVO;

                log.info("DEBUG statusConta={} suspensaoAte={}",
                        cliente.getStatusConta(),
                        cliente.getSuspensaoAte());

                // BLOQUEIO — conta suspensa impede o login com prazo de expiração
                if (status == StatusConta.SUSPENSO) {
                        String prazo = cliente.getSuspensaoAte() != null
                                ? "até " + cliente.getSuspensaoAte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : "por tempo indefinido";
                        log.info("DEBUG lançando CONTA_SUSPENSA para {}", email);
                        throw new IllegalArgumentException("CONTA_SUSPENSA|Sua conta está suspensa " + prazo + ". Em caso de dúvidas, entre em contato com a plataforma.");
                }

                if (status == StatusConta.REMOVIDO) {
                        log.info("DEBUG lançando CONTA_REMOVIDA para {}", email);
                        throw new IllegalArgumentException("CONTA_REMOVIDA|Sua conta foi removida da plataforma. Em caso de dúvidas, entre em contato com a plataforma.");
                }

                if (!cliente.isEmailVerificado()) {
                        logAuditoria.registrarLog(
                                AcaoAuditoria.LOGIN_FALHA.name(),
                                null,
                                email,
                                "Login falhou: e-mail não verificado");

                        throw new IllegalArgumentException("E-mail não verificado.");
                }

                if (cliente.isBloqueada()) {
                        logAuditoria.registrarLog(
                                AcaoAuditoria.LOGIN_BLOQUEADO.name(),
                                cliente.getId(),
                                email,
                                "Tentativa de login em conta bloqueada");

                        throw new IllegalArgumentException("Conta bloqueada.");
                }

                // BLOQUEIO — senha errada incrementa contador; conta bloqueada após 5 tentativas falhas
                if (!passwordEncoder.matches(senha, cliente.getSenha())) {

                        repositoryService.registrarFalhaLogin(cliente);

                        // Calcula quantas tentativas o usuário ainda tem antes do bloqueio total
                        int restantes = Math.max(0, 5 - cliente.getTentativas());

                        logAuditoria.registrarLog(
                                AcaoAuditoria.LOGIN_FALHA.name(),
                                cliente.getId(),
                                email,
                                "Senha incorreta. Tentativas restantes: " + restantes);

                        throw new IllegalArgumentException(
                                "Senha incorreta. Restam " + restantes + " tentativa(s).");
                }

                // LOGIN OK — zera o contador de tentativas falhas ao autenticar com sucesso
                repositoryService.resetarTentativasLogin(cliente);

                logAuditoria.registrarLog(
                        AcaoAuditoria.LOGIN_SUCESSO.name(),
                        cliente.getId(),
                        email,
                        "Login realizado com sucesso");

                return cliente;
        }

        /** Gera um token de recuperação de senha e registra o evento na auditoria. */
        public void gerarTokenRecuperacao(Cliente cliente) {
                senhaService.iniciarRecuperacao(cliente);

                logAuditoria.registrarLog(
                        AcaoAuditoria.RECUPERACAO_SENHA_SOLICITADA.name(),
                        cliente.getId(),
                        cliente.getEmail(),
                        "Token de recuperação de senha gerado");
        }

        /**
         * Redefine a senha do cliente usando o token de recuperação, desbloqueia a conta e zera tentativas.
         * Deleta o token após uso para evitar reutilização.
         */
        @Transactional
        public Cliente redefinirSenha(String token, String novaSenha) {

                RecuperacaoSenha registro = tokenRepository.findByToken(token)
                                .orElseThrow(() -> new IllegalArgumentException("Token inválido."));

                if (registro.isExpirado()) {
                        tokenRepository.delete(registro);

                        logAuditoria.registrarLog(
                                AcaoAuditoria.RECUPERACAO_SENHA_EXPIRADA.name(),
                                registro.getCliente().getId(),
                                registro.getCliente().getEmail(),
                                "Tentativa de uso de token expirado");

                        throw new IllegalArgumentException("Token expirado.");
                }

                Cliente cliente = registro.getCliente();

                cliente.setSenha(passwordEncoder.encode(novaSenha));
                cliente.setBloqueada(false);
                cliente.setTentativas(0);

                repositoryService.salvar(cliente);
                tokenRepository.delete(registro);

                logAuditoria.registrarLog(
                        AcaoAuditoria.RECUPERACAO_SENHA_CONCLUIDA.name(),
                        cliente.getId(),
                        cliente.getEmail(),
                        "Senha redefinida com sucesso");

                return cliente;
        }

        /**
         * Altera a senha do cliente logado após confirmar que a senha atual está correta.
         * Registra falha na auditoria caso a senha informada não confira.
         */
        @Transactional
        public void alterarSenha(String email, String senhaAtual, String novaSenha) {

                Cliente cliente = repositoryService.buscarPorEmailOuFalhar(email);

                if (!passwordEncoder.matches(senhaAtual, cliente.getSenha())) {

                        logAuditoria.registrarLog(
                                AcaoAuditoria.ALTERACAO_SENHA_FALHA.name(),
                                cliente.getId(),
                                email,
                                "Tentativa de alteração com senha atual incorreta");

                        throw new IllegalArgumentException("Senha atual incorreta.");
                }

                cliente.setSenha(passwordEncoder.encode(novaSenha));
                repositoryService.salvar(cliente);

                logAuditoria.registrarLog(
                        AcaoAuditoria.ALTERACAO_SENHA.name(),
                        cliente.getId(),
                        email,
                        "Senha alterada com sucesso");
        }

        /** Verifica se o token de recuperação de senha existe e ainda não expirou. */
        public boolean validarToken(String token) {
                return tokenRepository.findByToken(token)
                                .map(t -> !t.isExpirado())
                                .orElse(false);
        }
}