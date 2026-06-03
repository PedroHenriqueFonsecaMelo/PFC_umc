package umc.exs.service.email;

public interface EmailService {
    void enviar(String destino, String assunto, String texto);
    void enviarHtml(String destino, String assunto, String html) throws Exception;
}
