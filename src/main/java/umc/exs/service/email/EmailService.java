package umc.exs.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String remetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Envia e-mail em texto puro de forma assíncrona. */
    @Async("emailExecutor")
    public void enviar(String destino, String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destino);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        mensagem.setFrom(remetente);
        mailSender.send(mensagem);
    }

    /** Envia e-mail com corpo HTML de forma assíncrona. */
    @Async("emailExecutor")
    public void enviarHtml(@NonNull String destino, @NonNull String assunto, @NonNull String html)
            throws MessagingException {
        MimeMessage mensagem = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
        helper.setTo(destino);
        helper.setSubject(assunto);
        helper.setFrom(remetente);
        helper.setText(html, true);
        mailSender.send(mensagem);
    }
}
