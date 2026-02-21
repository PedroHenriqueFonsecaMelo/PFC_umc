package umc.exs.model.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// Importações de ClienteConvertible e Cliente removidas

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginDTO {
    
    // Adicionando @NotBlank para garantir que o usuário preencha os campos
    @NotBlank(message = "Email é obrigatório")
    private String email;
    
    @NotBlank(message = "Senha é obrigatória")
    private String senha;

    // Métodos toEntity() e ClienteConvertible foram removidos.
    // O Service de Autenticação usará diretamente getEmail() e getSenha().
}