package umc.exs.service.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLogger.class);

    public void loginSuccess(String username) {
        logger.info("SECURITY LOGIN SUCCESS - user={}", username);
    }

    public void loginFailure(String username, String reason) {
        logger.warn("SECURITY LOGIN FAILURE - user={} reason={}", username, reason);
    }

    public void accountBlocked(String username) {
        logger.error("SECURITY ACCOUNT BLOCKED - user={}", username);
    }
}