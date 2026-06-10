package umc.exs.service.log;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SecurityLoggerTest {

    SecurityLogger logger = new SecurityLogger();

    @Test
    void loginSuccess_naoDeveLancarErro() {
        assertDoesNotThrow(() -> {
            logger.loginSuccess("usuario");
        });
    }

    @Test
    void loginFailure_naoDeveLancarErro() {
        assertDoesNotThrow(() -> {
            logger.loginFailure("usuario", "senha incorreta");
        });
    }

    @Test
    void accountBlocked_naoDeveLancarErro() {
        assertDoesNotThrow(() -> {
            logger.accountBlocked("usuario");
        });
    }
}