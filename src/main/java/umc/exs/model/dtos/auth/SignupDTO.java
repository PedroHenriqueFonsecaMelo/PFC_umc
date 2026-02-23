package umc.exs.model.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para captura de dados no registro de novos clientes.
 * Focado apenas no transporte e validação básica de campos.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SignupDTO {

    @NotBlank(message = "O CPF é obrigatório.")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "Formato de CPF inválido (000.000.000-00).")
    private String cpf;
    
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Por favor, insira um e-mail válido.")
    private String email;
    
    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
    private String senha; 

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
    private String nome;

    @NotBlank(message = "A data de nascimento é obrigatória.")
    private String datanasc;

    @NotBlank(message = "O gênero deve ser informado.")
    private String gen;

    // Nota: A validação de 'true' para estes campos geralmente é feita no Controller
    // para permitir que o usuário receba uma mensagem específica de erro.
    private Boolean termsAccepted;
    private Boolean privacyAccepted;

    @Override
    public String toString() {
        return "SignupDTO{" +
                "cpf='" + cpf + '\'' +
                ", email='" + email + '\'' +
                ", nome='" + nome + '\'' +
                ", datanasc='" + datanasc + '\'' +
                ", gen='" + gen + '\'' +
                ", termsAccepted=" + termsAccepted +
                ", privacyAccepted=" + privacyAccepted +
                '}';
    }
}