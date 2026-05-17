package umc.exs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import umc.exs.dto.compra.cupom.CupomDTO;
import umc.exs.model.entidades.foundation.Cupom;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CupomMapper {

    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "clienteEmail", source = "cliente.email")
    CupomDTO toDTO(Cupom cupom);
}