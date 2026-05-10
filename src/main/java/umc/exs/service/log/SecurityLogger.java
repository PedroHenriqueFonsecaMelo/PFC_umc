package umc.exs.service.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLogger.class);

    public void loginSuccess(String username) {
        logger.info("AUDIT: LOGIN SUCESSO - Usuário '{}' logado com sucesso.", username);
    }

    public void loginFailure(String username, String reason) {
        logger.warn("AUDIT: LOGIN FALHA - Usuário '{}'. Motivo: {}", username, reason);
    }

    public void accountBlocked(String username) {
        logger.error("AUDIT: CONTA BLOQUEADA - A conta do usuário '{}' foi bloqueada após falhas.", username);
    }
}
