package umc.exs.model.daos.mappers;

import java.util.stream.Collectors;

import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.entidades.foundation.enums.Genero;
import umc.exs.model.entidades.usuario.Cliente;

public class ClienteMapper {
    
    /**
     * Converte Entidade Cliente para ClienteDTO.
     */
    public static ClienteDTO fromEntity(Cliente cliente) {
        if (cliente == null)
            return null;
            
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNome(cliente.getNome());
        dto.setEmail(cliente.getEmail());
        dto.setDatanasc(cliente.getDatanasc());
        
        // 1. ALTERAÇÃO: Conversão de Genero (Enum) para String (DTO) ou vice-versa,
        // dependendo do tipo definido no ClienteDTO. Se o DTO usar String:
        dto.setGen(cliente.getGen() != null ? cliente.getGen().toString() : null); 
        
        dto.setCpf(cliente.getCpf());
        // SEGURANÇA: Nunca retornar a senha real do DB.
        dto.setSenha(null);

        // Mapeia Endereços (Set para List)
        if (cliente.getEnderecos() != null) {
            dto.setEnderecos(cliente.getEnderecos().stream()
                .map(EnderecoMapper::fromEntity)
                .collect(Collectors.toList()));
        }
        
        // Mapeia Cartões (Set para List)
        if (cliente.getCartoes() != null) {
            dto.setCartoes(cliente.getCartoes().stream()
                .map(CartaoMapper::fromEntity) 
                .collect(Collectors.toList()));
        }
        
        return dto;
    }

    /**
     * Converte ClienteConvertible (DTO) para Entidade Cliente.
     */
    public static Cliente toEntity(ClienteConvertible dto) {
        if (dto == null)
            return null;
            
        Cliente c = new Cliente();
        c.setId(dto.getId());
        c.setNome(dto.getNome());
        c.setEmail(dto.getEmail());
        c.setDatanasc(dto.getDatanasc());
        
        // 2. ALTERAÇÃO CRÍTICA: Converte String do DTO para a Enum Genero da Entidade.
        c.setGen(dto.getGen() != null ? Genero.valueOf(dto.getGen().toUpperCase()) : null);
        
        c.setCpf(dto.getCpf());
        // Senha: Deve ser tratada no Service (hashing/verificação), não no Mapper.
        c.setSenha(dto.getSenha()); 

        // Mapeia Endereços (List para Set)
        if (dto.getEnderecos() != null) {
            c.setEnderecos(dto.getEnderecos().stream()
                .map(EnderecoMapper::toEntity)
                .collect(Collectors.toSet()));
        }
        
        // Mapeia Cartões (List para Set)
        if (dto.getCartoes() != null) {
            c.setCartoes(dto.getCartoes().stream()
                .map(CartaoMapper::toEntity)
                .collect(Collectors.toSet()));
        }

        return c;
    }

    /**
     * Converte SignupDTO (registro) para Entidade Cliente.
     */
    public static Cliente toEntity(SignupDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Cliente c = new Cliente();
        c.setCpf(dto.getCpf());
        c.setEmail(dto.getEmail());
        c.setNome(dto.getNome());
        c.setDatanasc(dto.getDatanasc());
        
        // 3. ALTERAÇÃO CRÍTICA: Converte String do SignupDTO para a Enum Genero.
        c.setGen(dto.getGen() != null ? Genero.valueOf(dto.getGen().toUpperCase()) : null);
        
        // A SENHA DEVE SER HASHADA/CRIPTOGRAFADA no Service antes de chamar o Repository.
        c.setSenha(dto.getSenha()); 
        
        return c;
    }
}