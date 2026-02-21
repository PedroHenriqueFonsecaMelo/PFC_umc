package umc.exs.service;

public interface EmailSender {
    void enviar(String destino, String assunto, String texto);
}
