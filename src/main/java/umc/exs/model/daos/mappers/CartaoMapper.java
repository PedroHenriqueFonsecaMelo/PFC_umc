package umc.exs.model.daos.mappers;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;

@Mapper(componentModel = "spring")
public interface CartaoMapper {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientes", ignore = true)
    @Mapping(target = "validade", source = "validade") // O MapStruct usará o método toValidadeString abaixo
    Cartao toEntity(CartaoDTO dto);

    @Mapping(target = "cvv", ignore = true)
    @Mapping(target = "validade", source = "validade") // O MapStruct usará o método toYearMonth abaixo
    CartaoDTO toDTO(Cartao cartao);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientes", ignore = true)
    void updateEntityFromDto(CartaoDTO dto, @MappingTarget Cartao entity);

    // --- MÉTODOS DE CONVERSÃO ACESSÍVEIS PELO SERVICE ---

    default String toValidadeString(YearMonth value) {
        return value != null ? value.format(FORMATTER) : null;
    }

    default YearMonth toYearMonth(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return YearMonth.parse(value, FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}