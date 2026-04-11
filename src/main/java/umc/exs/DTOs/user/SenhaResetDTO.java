package umc.exs.DTOs.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SenhaResetDTO {

    private String token;
    private String novaSenha;
    private String confirmarSenha;
}
