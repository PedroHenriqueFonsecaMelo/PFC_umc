package umc.exs.dto.request.cliente;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SenhaResetRequest {

    private String token;
    private String novaSenha;
    private String confirmarSenha;
}
