package umc.exs.model.daos.mappers;

import umc.exs.model.dtos.interfaces.EnderecoConvertible;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Endereco;

public class EnderecoMapper {

    public static EnderecoDTO fromEntity(Endereco endereco) {
        return EnderecoDTO.fromEntity(endereco);
    }
    public static Endereco toEntity(EnderecoConvertible enderecoDTO) {
        if (enderecoDTO == null)
            return null;
        return enderecoDTO.toEntity();
    }
}