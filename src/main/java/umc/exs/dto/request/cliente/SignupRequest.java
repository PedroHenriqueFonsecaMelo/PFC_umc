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

/**
 * DTO de cadastro de novo cliente com validações de CPF, e-mail, senha e data de nascimento.
 * Exige aceite obrigatório dos termos de uso e da política de privacidade antes do registro.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {

    // CPF do cliente no formato 000.000.000-00 ou apenas números; obrigatório
    @NotBlank(message = "O CPF é obrigatório.")
    @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF inválido. Use 000.000.000-00 ou apenas números.")
    private String cpf;

    // E-mail válido do cliente; obrigatório
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Insira um e-mail válido.")
    private String email;

    // Senha do cliente; obrigatória, mínimo 8 caracteres
    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
    private String senha;

    // Confirmação da senha; deve ser idêntica ao campo senha
    private String confirmPassword;

    // Nome completo do cliente; obrigatório
    @NotBlank(message = "O nome é obrigatório.")
    private String nome;

    // Data de nascimento no formato dd/MM/yyyy; obrigatória
    @NotNull(message = "A data de nascimento é obrigatória.")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate datanasc;

    // Gênero do cliente; opcional
    private String gen;

    // Aceite obrigatório dos termos de uso; deve ser true para prosseguir com o cadastro
    @AssertTrue(message = "Você deve aceitar os termos de uso.")
    private Boolean termsAccepted;

    // Aceite obrigatório da política de privacidade; deve ser true para prosseguir com o cadastro
    @AssertTrue(message = "Você deve aceitar a política de privacidade.")
    private Boolean privacyAccepted;

    /**
     * Valida se senha e confirmPassword são iguais.
     * Invocado automaticamente pelo Bean Validation via anotação @AssertTrue.
     */
    @AssertTrue(message = "As senhas não coincidem")
    public boolean isSenhaValida() {
        if (senha == null || confirmPassword == null)
            return false;
        return senha.equals(confirmPassword);
    }
}
