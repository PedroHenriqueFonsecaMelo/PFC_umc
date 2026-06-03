package umc.exs.service.email.facade;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import umc.exs.service.email.EmailService;

@Slf4j
@Service
public class EmailFacade {

    // O Spring injetará AUTOMATICAMENTE a única implementação ativa do Profile atual
    private final EmailService emailService;

    public EmailFacade(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendHtmlSafe(String to, String subject, String body) {
        try {
            if (to == null || to.isBlank()) {
                log.warn("Email ignorado: destinatário inválido");
                return;
            }

            emailService.enviarHtml(to, subject, body);

        } catch (Exception e) {
            log.error("Erro ao enviar email para {}", to, e);
        }
    }
}