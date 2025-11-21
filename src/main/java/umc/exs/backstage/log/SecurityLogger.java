package umc.exs.backstage.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLogger.class);

    /** 
     * @param username
     */
    public void Slogin(String username) {
        logger.info("AUDIT: LOGIN SUCESSO - Usuário '{}' logado com sucesso.", username);
    }

    /** 
     * @param username
     * @param reason
     */
    public void Flogin(String username, String reason) {
        logger.warn("AUDIT: LOGIN FALHA - Usuário '{}'. Motivo: {}", username, reason);
    }

    /** 
     * @param username
     */
    public void CBloqueada(String username) {
        logger.error("AUDIT: CONTA BLOQUEADA - A conta do usuário '{}' foi bloqueada após falhas.", username);
    }
}