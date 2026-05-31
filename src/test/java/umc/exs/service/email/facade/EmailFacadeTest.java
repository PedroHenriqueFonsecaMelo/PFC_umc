package umc.exs.service.email.facade;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.service.email.EmailService;

@ExtendWith(MockitoExtension.class)
class EmailFacadeTest {

    @Mock
    EmailService emailService;

    @InjectMocks
    EmailFacade facade;

    @Test
    void sendHtmlSafe_quandoDestinatarioInvalido_naoEnvia() {
        facade.sendHtmlSafe("", "Assunto", "Corpo");
        verifyNoInteractions(emailService);
    }

    @Test
    void sendHtmlSafe_quandoEnviaErro_naoPropagaExcecao() throws Exception {
        doThrow(new RuntimeException("erro")).when(emailService)
                .enviarHtml(anyString(), anyString(), anyString());

        facade.sendHtmlSafe("user@test.com", "Assunto", "Corpo");
        verify(emailService).enviarHtml("user@test.com", "Assunto", "Corpo");
    }
}
