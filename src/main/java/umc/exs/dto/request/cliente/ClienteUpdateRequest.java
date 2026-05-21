package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.usuario.Endereco;

import java.util.List;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateRequest {

    @NotBlank(message = "O nome é obrigatório.")
    private String nome;

    @Email(message = "Informe um e-mail válido.")
    private String email;

    @Past(message = "A data de nascimento deve ser no passado.")
    private LocalDate datanasc;

    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.")
    private String senha;

    private List<Endereco> enderecos;
}
