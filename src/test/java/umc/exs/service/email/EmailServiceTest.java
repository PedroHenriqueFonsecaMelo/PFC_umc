package umc.exs.service.email;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    // Dependência para o teste do SMTP
    @Mock
    private JavaMailSender mailSender;

    private EmailServiceSmtp smtpService;
    private EmailServiceGmailAPI gmailService;

    @BeforeEach
    void setUp() {
        // Inicializa o serviço SMTP passando o mock esperado por ele
        smtpService = new EmailServiceSmtp(mailSender);

        // Inicializa o serviço do Gmail passando credenciais fictícias em Base64 para evitar IllegalStateException
        // "eyA... " simula um JSON básico criptografado em Base64 para passar pelas validações de inicialização
        String fakeBase64Json = "eyJjbGllbnRfaWQiOiJmYWtlIiwidG9rZW5fdXJpIjoiaHR0cHM6Ly9mYWtlLmdvb2dsZS5jb20iLCJjbGllbnRfc2VjcmV0IjoiZmFrZSJ9";
        gmailService = new EmailServiceGmailAPI(fakeBase64Json, "fake-refresh-token");
    }

    @Test
    void enviar_deveEnviarMensagemSimplesViaSmtp() {
        // --- ARRANGE ---
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // --- ACT ---
        smtpService.enviar("destino@test.com", "Assunto", "Texto");

        // --- ASSERT ---
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarHtml_deveExecutarDisparoViaSmtp() throws Exception {
        ReflectionTestUtils.setField(smtpService, "remetente", "remetente@test.com");
        // --- ARRANGE ---
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // --- ACT ---
        smtpService.enviarHtml("destino@test.com", "Assunto", "<b>Html</b>");

        // --- ASSERT ---
        verify(mailSender, times(1)).send(mime);
    }
}