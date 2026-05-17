package umc.exs.mappers;

import org.mapstruct.Mapper;

import umc.exs.dto.compra.cupom.CupomValidacaoDTO;

@Mapper(componentModel = "spring")
public interface CupomValidacaoMapper {

    // Mantemos o cálculo no service e usamos MapStruct como DTO builder simples.
    CupomValidacaoDTO toDTO(boolean valido,
                              Double percentual,
                              Double precoOriginal,
                              Double precoComDesconto,
                              Double economia,
                              String mensagem);
}

