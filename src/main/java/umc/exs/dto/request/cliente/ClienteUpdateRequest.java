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

/**
 * DTO usado pelo cliente para atualizar seus dados pessoais no perfil.
 * Todos os campos são opcionais — apenas os informados são atualizados pelo service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateRequest {

    // Novo nome do cliente; mínimo 2 caracteres
    @Size(min = 2, message = "O nome deve ter pelo menos 2 caracteres.")
    private String nome;

    // Novo e-mail válido para substituir o e-mail atual do cliente
    @Email(message = "Informe um e-mail válido.")
    private String email;

    // Data de nascimento no passado no formato dd/MM/yyyy
    @Past(message = "A data de nascimento deve ser no passado.")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate datanasc;

    // Nova senha do cliente; mínimo 8 caracteres
    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.")
    private String senha;

    // Lista de endereços a atualizar; cada item é validado individualmente
    private List<@Valid EnderecoShared> enderecos;
}
