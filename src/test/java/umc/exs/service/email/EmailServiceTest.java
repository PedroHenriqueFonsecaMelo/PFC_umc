package umc.exs.service.email;

import static org.mockito.Mockito.*;

import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @InjectMocks
    EmailService service;

    @Test
    void enviar_deveEnviarMensagemSimples() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        service.enviar("destino@test.com", "Assunto", "Texto");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarHtml_deveCriarMimeMessage() throws Exception {
        ReflectionTestUtils.setField(service, "remetente", "from@test.com");
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);

        service.enviarHtml("destino@test.com", "Assunto", "<b>Html</b>");

        verify(mailSender).send(mime);
    }
}
