package umc.exs.model.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.entidades.usuario.Cliente;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SignupDTO implements ClienteConvertible {

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;
    @NotBlank(message = "Email é obrigatório")
    private String email;
    private String senha;
    private String nome;
    private String datanasc;
    private String gen;
    private Boolean termsAccepted;
    private Boolean privacyAccepted;

    @Override
    public Cliente toEntity() {
        Cliente c = new Cliente();
        c.setCpf(cpf);
        c.setEmail(email);
        c.setSenha(senha);
        c.setNome(nome);
        c.setDatanasc(datanasc);
        c.setGen(gen);
        return c;
    }

    @Override
    public String toString() {
        return "SignupDTO [cpf=" + cpf + ", email=" + email + ", nome=" + nome + ", datanasc=" + datanasc + ", gen="
                + gen + ", termsAccepted=" + termsAccepted + ", privacyAccepted=" + privacyAccepted + "]";
    }
}