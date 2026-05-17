package umc.exs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.livro.LivroDTO;
import umc.exs.model.entidades.livro.Livro;

@Mapper(componentModel = "spring")
public interface LivroMapper {

    @Mapping(target = "lote", ignore = true)
    @Mapping(target = "vendedor", ignore = true)
    @Mapping(target = "avaliacoes", ignore = true)
    @Mapping(target = "obra", ignore = true)
    Livro paraEntidade(LivroDTO dto);

    LivroDTO paraDTO(Livro entity);
}