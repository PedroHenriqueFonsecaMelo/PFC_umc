package umc.exs.service.email.facade;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.mail.MessagingException;
import umc.exs.service.email.EmailService;

@ExtendWith(MockitoExtension.class)
class EmailFacadeTest {

    private EmailFacade facade;

    @Mock
    private EmailService emailServiceMock;

    @BeforeEach
    void setUp() {
        this.facade = new EmailFacade(emailServiceMock);
    }

    @Test
    void deveUsarSmtpQuandoForOBeanAtivo() throws MessagingException {
        // Act
        facade.sendHtmlSafe("teste@uol.com", "Assunto", "Corpo");

        // Assert - Garante que a Facade repassou a chamada para o serviço de e-mail injetado
        try {
            verify(emailServiceMock, times(1)).enviarHtml("teste@uol.com", "Assunto", "Corpo");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void naoDeveEnviarEmailSeOStringDoDestinatarioForInvalido() throws MessagingException {
        // Act
        facade.sendHtmlSafe("", "Assunto", "Corpo");

        // Assert - Garante que o fluxo parou no "if (to.isBlank())" e o serviço nunca foi chamado
        verifyNoInteractions(emailServiceMock);
    }
}
