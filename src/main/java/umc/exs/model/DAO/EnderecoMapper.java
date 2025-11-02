package umc.exs.model.DAO;

import umc.exs.model.DTO.user.EnderecoDTO;
import umc.exs.model.entidades.Endereco;

public class EnderecoMapper {
    public static EnderecoDTO fromEntity(Endereco e) {
        if (e == null) return null;
        EnderecoDTO dto = new EnderecoDTO();
        dto.setId(e.getId() == 0 ? null : e.getId());
        dto.setPais(e.getPais());
        dto.setCep(e.getCep());
        dto.setEstado(e.getEstado());
        dto.setCidade(e.getCidade());
        dto.setRua(e.getRua());
        dto.setBairro(e.getBairro());
        dto.setNumero(String.valueOf(e.getNumero()));
        dto.setComplemento(e.getComplemento());
        dto.setTipoResidencia(e.getTipoResidencia());
        return dto;
    }

    public static Endereco toEntity(EnderecoDTO dto) {
        if (dto == null) return null;
        Endereco e = new Endereco();
        if (dto.getId() != null) e.setId((long) dto.getId().intValue());
        e.setPais(dto.getPais());
        e.setCep(dto.getCep());
        e.setEstado(dto.getEstado());
        e.setCidade(dto.getCidade());
        e.setRua(dto.getRua());
        e.setBairro(dto.getBairro());
        if (dto.getNumero() != null) e.setNumero(dto.getNumero());
        e.setComplemento(dto.getComplemento());
        e.setTipoResidencia(dto.getTipoResidencia());
        return e;
    }
}