package umc.exs.service.email;

public interface EmailSender {
    void enviar(String destino, String assunto, String texto);
}
