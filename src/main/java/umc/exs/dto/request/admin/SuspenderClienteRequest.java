package umc.exs.dto.request.admin;

import lombok.Data;

@Data
public class SuspenderClienteRequest {
    private String motivo;
    /** 0 = indefinido; valores válidos: 7, 15, 30 ou 0 */
    private int diasSuspensao;
    private boolean notificarEmail;
}
