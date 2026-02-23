package umc.exs.model.daos.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Endereco;

@Mapper(componentModel = "spring")
public interface EnderecoMapper {

    @Mapping(target = "clientes", ignore = true) // Evita loop infinito
    Endereco toEntity(EnderecoDTO dto);

    EnderecoDTO toDTO(Endereco endereco);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientes", ignore = true)
    void updateEntityFromDto(EnderecoDTO dto, @MappingTarget Endereco entity);
}