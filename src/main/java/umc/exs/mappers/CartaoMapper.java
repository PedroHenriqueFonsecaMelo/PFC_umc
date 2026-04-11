package umc.exs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import umc.exs.DTOs.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;

@Mapper(componentModel = "spring", uses = { DateMapper.class })
public interface CartaoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientes", ignore = true)
    @Mapping(target = "validade", qualifiedByName = "yearMonthToString")
    Cartao paraEntidade(CartaoDTO dto);

    @Mapping(target = "cvv", ignore = true)
    @Mapping(target = "validade", qualifiedByName = "stringToYearMonth")
    CartaoDTO paraDTO(Cartao cartao);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientes", ignore = true)
    // Removed source="validade" as it is redundant and can sometimes confuse the
    // processor
    @Mapping(target = "validade", qualifiedByName = "yearMonthToString")
    void atualizarEntidadeDeDto(CartaoDTO dto, @MappingTarget Cartao entity);
}