package umc.exs.dto.request.admin;

import lombok.Data;

/**
 * DTO usado pelo admin para remover permanentemente a conta de um cliente.
 * Informa o motivo da remoção e se o cliente deve ser notificado por e-mail.
 */
@Data
public class RemoverClienteRequest {

    // Justificativa da remoção registrada no sistema para fins de auditoria
    private String motivo;

    // Se true, envia e-mail ao cliente informando que sua conta foi removida
    private boolean notificarEmail;
}
