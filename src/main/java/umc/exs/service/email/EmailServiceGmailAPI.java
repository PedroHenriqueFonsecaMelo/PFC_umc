package umc.exs.service.email;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.Properties;

@Service
@Profile({"prod", "staging", "render"})
public class EmailServiceGmailAPI implements EmailService {

    private final String base64Credentials;
    private final String refreshToken;

    // Injeta tanto a credencial base64 quanto o refresh token obtido no Playground
    public EmailServiceGmailAPI(
            @Value("${gmail.api.credentials-base64:#{null}}") String base64Credentials,
            @Value("${gmail.api.refresh-token:#{null}}") String refreshToken) {
        this.base64Credentials = base64Credentials;
        this.refreshToken = refreshToken;
    }

    /**
     * Inicializa o cliente da API do Gmail usando o ClientSecrets + Refresh Token
     */
    private Gmail getGmailService() throws Exception {
        if (base64Credentials == null || base64Credentials.isEmpty()) {
            throw new IllegalStateException("A variável GMAIL_API_CREDENTIALS_BASE64 não foi configurada.");
        }

        String tokenFinal = (refreshToken != null) ? refreshToken : System.getenv("GMAIL_REFRESH_TOKEN");
        if (tokenFinal == null || tokenFinal.isEmpty()) {
            throw new IllegalStateException("O GMAIL_REFRESH_TOKEN não foi configurado nas variáveis de ambiente.");
        }

        byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Credentials.trim());

        try (ByteArrayInputStream in = new ByteArrayInputStream(decodedBytes)) {

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(),
                    new InputStreamReader(in));

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            ClientParametersAuthentication clientAuth = new ClientParametersAuthentication(
                    clientSecrets.getDetails().getClientId(),
                    clientSecrets.getDetails().getClientSecret());

            Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(httpTransport)
                    .setJsonFactory(GsonFactory.getDefaultInstance())
                    .setTokenServerEncodedUrl(clientSecrets.getDetails().getTokenUri())
                    .setClientAuthentication(clientAuth)
                    .build();

            credential.setRefreshToken(tokenFinal);

            return new Gmail.Builder(httpTransport, GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("PFC_Projeto")
                    .build();
        }
    }

    /** Converte a estrutura do e-mail para o formato bruto (Raw) que a API exige */
    private Message createMessageWithEmail(MimeMessage mimeMessage) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /** Envia e-mail em texto puro de forma assíncrona. */
    @Override
    @Async("emailExecutor")
    public void enviar(String destino, String assunto, String texto) {
        dispararViaApi(destino, assunto, texto, false);
    }

    /** Envia e-mail com corpo HTML de forma assíncrona. */
    @Override
    @Async("emailExecutor")
    public void enviarHtml(String destino, String assunto, String html) {
        dispararViaApi(destino, assunto, html, true);
    }

    /** Centraliza a lógica de envio da API */
    private void dispararViaApi(String destino, String assunto, String conteudo, boolean isHtml) {
        try {
            Gmail service = getGmailService();

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);

            email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(destino));
            email.setSubject(assunto);

            if (isHtml) {
                email.setContent(conteudo, "text/html; charset=utf-8");
            } else {
                email.setText(conteudo);
            }

            Message message = createMessageWithEmail(email);

            // "me" faz o Gmail disparar usando a conta que gerou o Refresh Token
            service.users().messages().send("me", message).execute();
            System.out.println("E-mail enviado com sucesso via Gmail API!");

        } catch (Exception e) {
            System.err.println("Falha ao disparar e-mail via Gmail API: " + e.getMessage());
            e.printStackTrace();
        }
    }
}