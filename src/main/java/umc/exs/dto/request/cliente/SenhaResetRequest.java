package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO usado no fluxo de redefinição de senha via token enviado por e-mail.
 * Valida que a nova senha e a confirmação coincidem antes de persistir a alteração.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SenhaResetRequest {

    // Token de recuperação enviado ao e-mail do cliente; obrigatório
    @NotBlank(message = "O token é obrigatório")
    private String token;

    // Nova senha do cliente; obrigatória, entre 8 e 100 caracteres
    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
    private String novaSenha;

    // Confirmação da nova senha; deve ser idêntica ao campo novaSenha
    @NotBlank(message = "A confirmação de senha é obrigatória")
    private String confirmarSenha;

    /**
     * Valida se novaSenha e confirmarSenha são iguais.
     * Invocado automaticamente pelo Bean Validation via anotação @AssertTrue.
     */
    @AssertTrue(message = "As senhas não coincidem")
    public boolean isSenhasIguais() {
        if (novaSenha == null || confirmarSenha == null) {
            return false;
        }
        return novaSenha.equals(confirmarSenha);
    }
}
