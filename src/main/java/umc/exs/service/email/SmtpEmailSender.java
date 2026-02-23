package umc.exs.service.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.impl", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {

    private final EmailService delegate;

    public SmtpEmailSender(EmailService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void enviar(String destino, String assunto, String texto) {
        delegate.enviar(destino, assunto, texto);
    }
}
