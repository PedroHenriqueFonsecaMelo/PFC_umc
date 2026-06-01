package umc.exs.service.log;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppLogger {

    private final LogAuditoriaService audit;
    private final SecurityLogger security;

    public void info(AcaoAuditoria acao, Long userId, String email, String detalhes) {
        audit.registrarLog(acao.name(), userId, email, detalhes);
    }

    public void info(AcaoAuditoria acao, String detalhes) {
        audit.registrarLog(acao.name(), detalhes);
    }

    public void success(AcaoAuditoria acao, Long userId, String email, String detalhes) {
        audit.registrarLog(acao.name(), userId, email, detalhes);
    }

    public void error(AcaoAuditoria acao, Long userId, String email, String detalhes) {
        audit.registrarLog(acao.name(), userId, email, detalhes);
    }

    public void loginSuccess(String username) {
        security.loginSuccess(username);
        audit.registrarLog(AcaoAuditoria.AUTH_LOGIN_SUCESSO.name(), username, "Login efetuado");
    }

    public void loginFailure(String username, String reason) {
        security.loginFailure(username, reason);
        audit.registrarLog(AcaoAuditoria.AUTH_LOGIN_FALHA.name(), username, reason);
    }

    public void accountBlocked(String username) {
        security.accountBlocked(username);
        audit.registrarLog(AcaoAuditoria.AUTH_CONTA_BLOQUEADA.name(), username, "Conta bloqueada");
    }
}