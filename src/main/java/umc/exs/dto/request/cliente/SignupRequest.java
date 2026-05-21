package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "O CPF é obrigatório.")
    @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
             message = "CPF inválido. Use 000.000.000-00 ou apenas números.")
    private String cpf;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Insira um e-mail válido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
    private String senha;

    private String confirmPassword;

    @NotBlank(message = "O nome é obrigatório.")
    private String nome;

    @NotNull(message = "A data de nascimento é obrigatória.")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate datanasc;

    private String gen;

    @AssertTrue(message = "Você deve aceitar os termos de uso.")
    private Boolean termsAccepted;

    @AssertTrue(message = "Você deve aceitar a política de privacidade.")
    private Boolean privacyAccepted;

    @AssertTrue(message = "As senhas não coincidem")
    public boolean isSenhaValida() {
        if (senha == null || confirmPassword == null) return false;
        return senha.equals(confirmPassword);
    }
}