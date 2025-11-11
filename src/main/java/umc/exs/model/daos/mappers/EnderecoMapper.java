package umc.exs.model.daos.mappers;

import umc.exs.model.dtos.interfaces.EnderecoConvertible;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Endereco;

public class EnderecoMapper {

    /**
     * Converte Entidade Endereco para EnderecoDTO.
     */
    public static EnderecoDTO fromEntity(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        EnderecoDTO dto = new EnderecoDTO();
        dto.setId(endereco.getId());
        dto.setRua(endereco.getRua());
        dto.setNumero(endereco.getNumero());
        dto.setBairro(endereco.getBairro());
        dto.setCidade(endereco.getCidade());
        dto.setEstado(endereco.getEstado());
        dto.setCep(endereco.getCep());
        dto.setPais(endereco.getPais());
        dto.setComplemento(endereco.getComplemento());
        dto.setTipoResidencia(endereco.getTipoResidencia());
        
        return dto;
    }

    /**
     * Converte EnderecoConvertible (DTO) para Entidade Endereco.
     */
    public static Endereco toEntity(EnderecoConvertible dto) {
        if (dto == null) {
            return null;
        }
        Endereco e = new Endereco();
        e.setId(dto.getId());
        e.setRua(dto.getRua());
        e.setNumero(dto.getNumero());
        e.setBairro(dto.getBairro());
        e.setCidade(dto.getCidade());
        e.setEstado(dto.getEstado());
        e.setCep(dto.getCep());
        e.setPais(dto.getPais());
        e.setComplemento(dto.getComplemento());
        e.setTipoResidencia(dto.getTipoResidencia());
        
        return e;
    }
}