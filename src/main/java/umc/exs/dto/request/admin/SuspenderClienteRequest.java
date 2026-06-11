package umc.exs.dto.request.admin;

import lombok.Data;

/**
 * DTO usado pelo admin para suspender temporariamente a conta de um cliente.
 * Informa o motivo, a duração da suspensão e se o cliente deve ser notificado por e-mail.
 */
@Data
public class SuspenderClienteRequest {

    // Justificativa da suspensão registrada no sistema para fins de auditoria
    private String motivo;

    /** 0 = indefinido; valores válidos: 7, 15, 30 ou 0 */
    private int diasSuspensao;

    // Se true, envia e-mail ao cliente informando que sua conta foi suspensa
    private boolean notificarEmail;
}
