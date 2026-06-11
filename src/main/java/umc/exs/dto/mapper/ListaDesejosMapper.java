package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import umc.exs.dto.response.cliente.ListaDesejosResponse;
import umc.exs.model.entidades.foundation.ListaDesejos;

/**
 * Mapper MapStruct que converte entidades ListaDesejos para DTOs de resposta.
 * Mapeamento gerado automaticamente pelo MapStruct por correspondência de nomes.
 */
@Mapper(componentModel = "spring")
public interface ListaDesejosMapper {

    /**
     * Converte um item da lista de desejos para o DTO de resposta ListaDesejosResponse.
     * Mapeamento gerado automaticamente pelo MapStruct.
     */
    ListaDesejosResponse toDTO(ListaDesejos entity);

    /**
     * Converte uma lista de entidades ListaDesejos para uma lista de DTOs de resposta.
     * Aplica o mesmo mapeamento de toDTO() para cada item da lista.
     */
    List<ListaDesejosResponse> toDTOList(List<ListaDesejos> entities);
}
