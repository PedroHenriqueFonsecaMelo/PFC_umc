package umc.exs.service.email;

import com.google.api.services.gmail.Gmail;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailServiceGmailAPITest {

    private EmailServiceGmailAPI service;

    private static final String credentialsBase64 = "dGVzdA=="; // fake base64
    private static final String refreshToken = "fake-refresh-token";

    @BeforeEach
    void setup() {
        service = new EmailServiceGmailAPI(credentialsBase64, refreshToken);
    }

    // ================================
    // ❌ ERROS DE CONFIGURAÇÃO
    // ================================

    @Test
    void deveFalhar_QuandoCredenciaisNulas() {
        EmailServiceGmailAPI svc = new EmailServiceGmailAPI(null, refreshToken);

        Exception ex = assertThrows(IllegalStateException.class, () -> {
            invokeGetGmailService(svc);
        });

        assertTrue(ex.getMessage().contains("GMAIL_API_CREDENTIALS_BASE64"));
    }

    @Test
    void deveFalhar_QuandoRefreshTokenNulo() {
        EmailServiceGmailAPI svc = new EmailServiceGmailAPI(credentialsBase64, null);

        Exception ex = assertThrows(IllegalStateException.class, () -> {
            invokeGetGmailService(svc);
        });

        assertTrue(ex.getMessage().contains("GMAIL_REFRESH_TOKEN"));
    }

    // ================================
    // ✅ ENVIO (SEM EXPLODIR)
    // ================================

    @Test
    void enviarNaoDeveLancarExcecao() {
        assertDoesNotThrow(() -> {
            service.enviar("teste@email.com", "Assunto", "Texto");
        });
    }

    @Test
    void enviarHtmlNaoDeveLancarExcecao() {
        assertDoesNotThrow(() -> {
            service.enviarHtml("teste@email.com", "Assunto", "<h1>HTML</h1>");
        });
    }

    // ================================
    // 💥 ERRO INTERNO (MOCK)
    // ================================

    @Test
    void deveCapturarErroAoObterServicoGmail() throws Exception {

        EmailServiceGmailAPI spy = Mockito.spy(service);

        doThrow(new Exception("Erro simulado"))
                .when(spy)
                .getGmailService();

        assertDoesNotThrow(() -> spy.dispararViaApi(
                "teste@email.com",
                "Assunto",
                "Texto",
                false));
    }

    @Test
	public void EmailServiceGmailAPI1() {
		String base64Credentials = null;
		String refreshToken = null;
		EmailServiceGmailAPI expected = new EmailServiceGmailAPI(null, null);
		EmailServiceGmailAPI actual = new EmailServiceGmailAPI(base64Credentials, refreshToken);

		assertTrue(EqualsBuilder.reflectionEquals(expected, actual, false, null, true));
	}

    // ================================
    // 🔧 UTIL
    // ================================

    private Gmail invokeGetGmailService(EmailServiceGmailAPI svc) throws Exception {
        Method method = EmailServiceGmailAPI.class.getDeclaredMethod("getGmailService");
        method.setAccessible(true);

        try {
            return (Gmail) method.invoke(svc);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}