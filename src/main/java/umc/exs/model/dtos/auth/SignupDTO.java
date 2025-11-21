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

    /** 
     * @return String
     */
    // O método toEntity() foi removido daqui e será movido para o Mapper (veja abaixo).
    // O DTO deve ser apenas um contêiner de dados.

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SignupDTO{");
        sb.append("cpf=").append(cpf);
        sb.append(", email=").append(email);
        sb.append(", senha=").append(senha);
        sb.append(", nome=").append(nome);
        sb.append(", datanasc=").append(datanasc);
        sb.append(", gen=").append(gen);
        sb.append(", termsAccepted=").append(termsAccepted);
        sb.append(", privacyAccepted=").append(privacyAccepted);
        sb.append('}');
        return sb.toString();
    }

   
}