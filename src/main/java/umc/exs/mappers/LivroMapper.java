package umc.exs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import umc.exs.DTOs.livro.LivroRequestDTO;
import umc.exs.model.entidades.livro.Livro;

@Mapper(componentModel = "spring")
public interface LivroMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lote", ignore = true)
    @Mapping(target = "fotosUrls", ignore = true)
    @Mapping(target = "dataAnuncio", ignore = true)
    @Mapping(target = "aprovado", ignore = true)
    @Mapping(target = "precoAprovado", ignore = true)
    @Mapping(target = "estadoAprovado", ignore = true)
    @Mapping(target = "dataAprovacao", ignore = true)
    @Mapping(target = "adminAprovadorId", ignore = true)
    @Mapping(target = "resumoOficial", ignore = true)
    @Mapping(target = "vendedor", ignore = true)
    @Mapping(target = "avaliacoes", ignore = true)
    @Mapping(target = "obra", ignore = true)
    Livro paraEntidade(LivroRequestDTO dto);

    LivroRequestDTO paraDTO(Livro entity);
}