package umc.exs.backstage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailRecuperacaoSenha(String destinatario, String token) {
        String assunto = "Recuperação de Senha - UMC";
        String link = "https://seusite.com/clientes/reset-senha?token=" + token;

        String conteudo = """
                Olá!

                Você solicitou a recuperação de senha.
                Utilize o link abaixo para redefinir sua senha:

                """ + link + """

                Caso não tenha solicitado, ignore este e-mail.
                """;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject(assunto);
            message.setText(conteudo);
            message.setFrom("SEU_EMAIL_AQUI@gmail.com"); // importante!

            mailSender.send(message);

            System.out.println("---------------------------------------------------------------------");
            System.out.println("📧 E-mail real enviado para: " + destinatario);
            System.out.println("TOKEN: " + token);
            System.out.println("LINK: " + link);
            System.out.println("---------------------------------------------------------------------");

        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar e-mail: " + e.getMessage());
            throw new RuntimeException("Falha ao enviar e-mail", e);
        }
    }
}
