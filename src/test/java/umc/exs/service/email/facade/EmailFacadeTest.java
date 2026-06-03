package umc.exs.service.email.facade;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.MessagingException;
import umc.exs.service.email.EmailServiceGmailAPI;
import umc.exs.service.email.EmailServiceSmtp;

@ExtendWith(MockitoExtension.class)
class EmailFacadeTest {

    @Mock
    private EmailServiceGmailAPI gmailService;

    @Mock
    private EmailServiceSmtp smtpService;

    @InjectMocks
    private EmailFacade facade;

    @Test
    void deveUsarSmtpQuandoProfileForLocal() {
        // Arrange - Forçando o profile "local" via Reflection
        ReflectionTestUtils.setField(facade, "profile", "local");

        // Act
        facade.sendHtmlSafe("teste@uol.com", "Assunto", "Corpo");

        // Assert - Garante que o SMTP foi chamado e o Gmail API foi ignorado
        try {
            verify(smtpService, times(1)).enviarHtml(anyString(), anyString(), anyString());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        verifyNoInteractions(gmailService);
    }

    @Test
    void deveUsarGmailQuandoProfileForProd() {
        // Arrange - Forçando o profile "prod"
        ReflectionTestUtils.setField(facade, "profile", "prod");

        // Act
        facade.sendHtmlSafe("teste@uol.com", "Assunto", "Corpo");

        // Assert - Garante que o Gmail foi chamado e o SMTP ignorado
        verify(gmailService, times(1)).enviarHtml(anyString(), anyString(), anyString());
        verifyNoInteractions(smtpService);
    }
}