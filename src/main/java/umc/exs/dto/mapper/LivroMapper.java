package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.response.compras.LivroExibicaoResponse;
import umc.exs.model.entidades.livro.Livro;

@Mapper(componentModel = "spring")
public interface LivroMapper {

    @Mapping(source = "estadoAprovado", target = "estadoAprovado")
    @Mapping(source = "precoAprovado", target = "precoAprovado")
    @Mapping(source = "resumoOficial", target = "descricao")
    @Mapping(source = "fotosUrls", target = "fotoUrl")
    LivroExibicaoResponse toResponse(Livro livro);

    List<LivroExibicaoResponse> toResponseList(List<Livro> livros);
}
