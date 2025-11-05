package umc.exs.model.dtos.auth;

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
public class LoginDTO implements ClienteConvertible{
    private String email;
    private String senha;

    
    @Override
    public Cliente toEntity() {
        Cliente cliente = new Cliente();
        cliente.setEmail(this.email);
        cliente.setSenha(this.senha);
        return cliente;
    }

}