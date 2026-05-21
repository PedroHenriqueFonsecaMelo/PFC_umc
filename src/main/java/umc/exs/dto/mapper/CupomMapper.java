package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.response.compras.CupomResponse;
import umc.exs.model.entidades.foundation.Cupom;

@Mapper(componentModel = "spring")
public interface CupomMapper {

    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "clienteEmail", source = "cliente.email")
    CupomResponse toResponse(Cupom cupom);

    List<CupomResponse> toResponseList(List<Cupom> cupons);
}