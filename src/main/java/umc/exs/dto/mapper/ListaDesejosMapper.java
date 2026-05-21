package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import umc.exs.dto.response.cliente.ListaDesejosResponse;
import umc.exs.model.entidades.foundation.ListaDesejos;

@Mapper(componentModel = "spring")
public interface ListaDesejosMapper {

    ListaDesejosResponse toDTO(ListaDesejos entity);

    List<ListaDesejosResponse> toDTOList(List<ListaDesejos> entities);
}