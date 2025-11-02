package umc.exs.model.DAO;

import umc.exs.model.DTO.user.CartaoDTO;
import umc.exs.model.entidades.Cartao;

public class CartaoMapper {
    public static CartaoDTO fromEntity(Cartao c) {
        if (c == null) return null;
        CartaoDTO dto = new CartaoDTO();
        dto.setId(c.getId() == 0 ? null : c.getId());
        dto.setNumero(Long.parseLong(c.getNumero()) == 0 ? null : c.getNumero());
        dto.setBandeira(c.getBandeira());
        dto.setNomeTitular(c.getNomeTitular());
        dto.setValidade(null); // not available in legacy entity - keep null
        dto.setCvv(null); // never expose cvv
        dto.setPreferencial(c.isPreferencial() == true);
        return dto;
    }

    public static Cartao toEntity(CartaoDTO dto) {
        if (dto == null) return null;
        Cartao c = new Cartao();
        if (dto.getId() != null) c.setId(dto.getId());
        if (dto.getNumero() != null) c.setNumero(dto.getNumero());
        c.setBandeira(dto.getBandeira());
        c.setNomeTitular(dto.getNomeTitular());
        if (dto.isPreferencial()) c.setPreferencial(true); else c.setPreferencial(false);
        // cvv/validade handled via additional service in secure flows
        return c;
    }
}