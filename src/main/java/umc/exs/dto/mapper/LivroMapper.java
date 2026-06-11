package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.response.compras.LivroExibicaoResponse;
import umc.exs.model.entidades.livro.Livro;

/**
 * Mapper MapStruct que converte a entidade Livro para LivroExibicaoResponse para exibição na vitrine.
 * Inclui estado do exemplar, preço aprovado, dados de promoção e URLs das fotos.
 */
@Mapper(componentModel = "spring")
public interface LivroMapper {

    /**
     * Converte um Livro para o DTO de exibição com todos os campos da vitrine mapeados.
     * Mapeia descrição, fotos, estado, preço e informações de promoção.
     */
    @Mapping(source = "estadoAprovado", target = "estadoAprovado")
    @Mapping(source = "precoAprovado", target = "precoAprovado")
    @Mapping(source = "resumoOficial", target = "descricao")
    @Mapping(source = "fotosUrls", target = "fotoUrl")
    @Mapping(source = "fotosUrls", target = "fotosUrls")
    @Mapping(source = "emPromocao", target = "emPromocao")
    @Mapping(source = "precoOriginal", target = "precoOriginal")
    @Mapping(source = "promocaoExpira", target = "promocaoExpira")
    LivroExibicaoResponse toResponse(Livro livro);

    /**
     * Converte uma lista de livros para uma lista de DTOs de exibição da vitrine.
     * Aplica o mesmo mapeamento de toResponse() para cada item da lista.
     */
    List<LivroExibicaoResponse> toResponseList(List<Livro> livros);
}
