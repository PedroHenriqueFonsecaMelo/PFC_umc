package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SenhaResetRequest {

    @NotBlank(message = "O token é obrigatório")
    private String token;

    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
    private String novaSenha;

    @NotBlank(message = "A confirmação de senha é obrigatória")
    private String confirmarSenha;

    @AssertTrue(message = "As senhas não coincidem")
    public boolean isSenhasIguais() {
        if (novaSenha == null || confirmarSenha == null) {
            return false;
        }
        return novaSenha.equals(confirmarSenha);
    }
}
