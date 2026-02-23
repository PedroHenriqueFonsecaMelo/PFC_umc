package umc.exs.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.impl", havingValue = "noop")
public class NoopEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(NoopEmailSender.class);

    @Override
    public void enviar(String destino, String assunto, String texto) {
        logger.info("NoopEmailSender: ignoring send to {} subject={}", destino, assunto);
    }
}
