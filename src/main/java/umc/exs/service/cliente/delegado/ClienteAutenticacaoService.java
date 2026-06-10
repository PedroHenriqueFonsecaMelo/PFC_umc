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

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteAutenticacaoService {

        private final ClienteRepositoryService repositoryService;
        private final PasswordEncoder passwordEncoder;
        private final RecuperacaoSenhaRepository tokenRepository;
        private final SenhaService senhaService;
        private final LogAuditoriaService logAuditoria;

        @Transactional
        public Cliente autenticar(String email, String senha) {

                Cliente cliente = repositoryService.encontrarPorEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

                StatusConta status = cliente.getStatusConta() != null ? cliente.getStatusConta() : StatusConta.ATIVO;

                log.info("DEBUG statusConta={} suspensaoAte={}",
                                cliente.getStatusConta(),
                                cliente.getSuspensaoAte());

                if (status == StatusConta.SUSPENSO) {
                        String prazo = cliente.getSuspensaoAte() != null
                                        ? "até " + cliente.getSuspensaoAte()
                                                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        : "por tempo indefinido";
                        log.info("DEBUG lançando CONTA_SUSPENSA para {}", email);
                        throw new IllegalArgumentException("CONTA_SUSPENSA|Sua conta está suspensa " + prazo
                                        + ". Em caso de dúvidas, entre em contato com a plataforma.");
                }

                if (status == StatusConta.REMOVIDO) {
                        log.info("DEBUG lançando CONTA_REMOVIDA para {}", email);
                        throw new IllegalArgumentException(
                                        "CONTA_REMOVIDA|Sua conta foi removida da plataforma. Em caso de dúvidas, entre em contato com a plataforma.");
                }

                if (!cliente.isEmailVerificado()) {
                        logAuditoria.registrarLog(
                                        AcaoAuditoria.LOGIN_FALHA.name(),
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

                if (!passwordEncoder.matches(senha, cliente.getSenha())) {

                        repositoryService.registrarFalhaLogin(cliente);

                        int restantes = Math.max(0, 5 - cliente.getTentativas());

                        logAuditoria.registrarLog(
                                        AcaoAuditoria.LOGIN_FALHA.name(),
                                        cliente.getId(),
                                        email,
                                        "Senha incorreta. Tentativas restantes: " + restantes);

                        throw new IllegalArgumentException(
                                        "Senha incorreta. Restam " + restantes + " tentativa(s).");
                }

                repositoryService.resetarTentativasLogin(cliente);

                logAuditoria.registrarLog(
                                AcaoAuditoria.LOGIN_SUCESSO.name(),
                                cliente.getId(),
                                email,
                                "Login realizado com sucesso");

                return cliente;
        }

        public void gerarTokenRecuperacao(Cliente cliente) {
                senhaService.iniciarRecuperacao(cliente);

                logAuditoria.registrarLog(
                                AcaoAuditoria.RECUPERACAO_SENHA_SOLICITADA.name(),
                                cliente.getId(),
                                cliente.getEmail(),
                                "Token de recuperação de senha gerado");
        }

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

        public boolean validarToken(String token) {
                return tokenRepository.findByToken(token)
                                .map(t -> !t.isExpirado())
                                .orElse(false);
        }
}