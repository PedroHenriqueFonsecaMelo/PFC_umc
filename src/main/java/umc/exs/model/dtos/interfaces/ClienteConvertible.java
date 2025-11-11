package umc.exs.model.dtos.interfaces;

import java.util.List;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Cliente;

public interface ClienteConvertible {

    Cliente toEntity(); 
    ClienteDTO fromEntity (Cliente cliente);
    
    // Getters dos campos básicos
    Long getId();
    String getNome();
    String getEmail();
    String getDatanasc();
    String getGen();
    String getSenha();
    String getCpf();
    
    // Getters das coleções (List para DTO/Web)
    List<EnderecoDTO> getEnderecos();
    List<CartaoDTO> getCartoes();
}