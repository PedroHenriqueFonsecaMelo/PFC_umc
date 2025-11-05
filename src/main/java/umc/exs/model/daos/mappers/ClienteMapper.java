package umc.exs.model.daos.mappers;

import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.entidades.usuario.Cliente;

public class ClienteMapper {

    /** Converte qualquer DTO que implemente ClienteConvertible em Cliente */
    public static Cliente toEntity(ClienteConvertible dto) {
        if (dto == null) return null;
        return dto.toEntity();
    }

    /** Converte Cliente em ClienteDTO (usado para respostas) */
    public static ClienteDTO fromEntity(Cliente cliente) {
        return ClienteDTO.fromEntity(cliente);
    }
}
