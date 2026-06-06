package umc.exs.dto.request.cliente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateRequest {

    @Size(min = 2, message = "O nome deve ter pelo menos 2 caracteres.")
    private String nome;

    @Email(message = "Informe um e-mail válido.")
    private String email;

    @Past(message = "A data de nascimento deve ser no passado.")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate datanasc;

    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.")
    private String senha;

    private List<@Valid EnderecoShared> enderecos; 
}