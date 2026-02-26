package umc.exs.model.daos.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import umc.exs.model.dtos.LivroRequestDTO;
import umc.exs.model.entidades.foundation.LivroAnuncio;

@Mapper(componentModel = "spring")
public interface LivroMapper {

    LivroMapper INSTANCE = Mappers.getMapper(LivroMapper.class);

    // Converte o DTO recebido da Web para a Entidade que vai ao Banco
    @Mapping(target = "id", ignore = true) 
    @Mapping(target = "vendedor", ignore = true) 
    @Mapping(target = "fotoUrl", ignore = true) 
    @Mapping(target = "dataAnuncio", ignore = true)
    LivroAnuncio toEntity(LivroRequestDTO dto);

    // Converte a Entidade de volta para DTO (se precisar retornar para a tela)
    LivroRequestDTO toDto(LivroAnuncio entity);
}