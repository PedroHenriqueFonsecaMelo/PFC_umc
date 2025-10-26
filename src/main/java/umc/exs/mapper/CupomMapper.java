package umc.exs.mapper;

import umc.exs.model.DTO.CupomDTO;
import umc.exs.model.compras.Cupom;

public class CupomMapper {
    public static CupomDTO fromEntity(Cupom c) {
        if (c == null) return null;
        CupomDTO dto = new CupomDTO();
        dto.setId(c.getId());
        dto.setCodigo(c.getCodigo());
        dto.setValor(c.getValor());
        dto.setClienteId(c.getClienteId());
        dto.setExpiracao(c.getExpiracao());
        dto.setUsado(c.isUsado());
        return dto;
    }

    public static Cupom toEntity(CupomDTO dto) {
        if (dto == null) return null;
        Cupom c = new Cupom();
        c.setId(dto.getId());
        c.setCodigo(dto.getCodigo());
        c.setValor(dto.getValor());
        c.setClienteId(dto.getClienteId());
        c.setExpiracao(dto.getExpiracao());
        c.setUsado(dto.isUsado());
        return c;
    }
}