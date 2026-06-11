package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.response.compras.CupomResponse;
import umc.exs.model.entidades.foundation.Cupom;

/**
 * Mapper MapStruct que converte a entidade Cupom para CupomResponse.
 * Inclui o nome e o e-mail do cliente vinculado ao cupom no DTO de resposta.
 */
@Mapper(componentModel = "spring")
public interface CupomMapper {

    /**
     * Converte um Cupom para o DTO de resposta, mapeando nome e e-mail do cliente vinculado.
     * Os demais campos são mapeados automaticamente pelo MapStruct por nome.
     */
    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "clienteEmail", source = "cliente.email")
    CupomResponse toResponse(Cupom cupom);

    /**
     * Converte uma lista de cupons para uma lista de DTOs de resposta.
     * Aplica o mesmo mapeamento de toResponse() para cada item da lista.
     */
    List<CupomResponse> toResponseList(List<Cupom> cupons);
}
