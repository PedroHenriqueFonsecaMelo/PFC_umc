package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// Importações de ClienteConvertible e Cliente removidas

/**
 * DTO de autenticação enviado pelo cliente no formulário de login.
 * Contém e-mail e senha para validação pelo Spring Security.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    // E-mail do cliente; obrigatório e deve estar em formato válido
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    // Senha do cliente; obrigatória e não pode ser vazia
    @NotBlank(message = "Senha é obrigatória")
    private String senha;
}
