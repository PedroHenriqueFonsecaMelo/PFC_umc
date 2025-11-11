package umc.exs.model.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Importações de ClienteConvertible e Cliente removidas.

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SignupDTO { // Não implementa mais ClienteConvertible

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;
    
    @NotBlank(message = "Email é obrigatório")
    private String email;
    
    // Senha não precisa de @NotBlank se você for validar o comprimento/complexidade
    private String senha; 
    private String nome;
    private String datanasc;
    private String gen;
    private Boolean termsAccepted;
    private Boolean privacyAccepted;

    // O método toEntity() foi removido daqui e será movido para o Mapper (veja abaixo).
    // O DTO deve ser apenas um contêiner de dados.

    @Override
    public String toString() {
        // Mantido o toString simplificado
        return "SignupDTO [cpf=" + cpf + ", email=" + email + ", nome=" + nome + ", datanasc=" + datanasc + ", gen="
                + gen + ", termsAccepted=" + termsAccepted + ", privacyAccepted=" + privacyAccepted + "]";
    }
}