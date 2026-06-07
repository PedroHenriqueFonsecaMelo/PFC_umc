package umc.exs.dto.request.admin;

import lombok.Data;

@Data
public class RemoverClienteRequest {
    private String motivo;
    private boolean notificarEmail;
}
