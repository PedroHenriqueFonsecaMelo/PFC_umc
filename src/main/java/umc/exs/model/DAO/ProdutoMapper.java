package umc.exs.model.DAO;

import umc.exs.model.DTO.ProdutoDTO;
import umc.exs.model.foundation.Produto;

public class ProdutoMapper {
    public static ProdutoDTO fromEntity(Produto p) {
        if (p == null) return null;
        ProdutoDTO dto = new ProdutoDTO();
        dto.setId(p.getId());
        dto.setTitulo(p.getTitulo());
        dto.setPrecificacao(p.getPrecificacao());
        return dto;
    }

    public static Produto toEntity(ProdutoDTO dto) {
        if (dto == null) return null;
        Produto p = new Produto();
        p.setId(dto.getId());
        p.setTitulo(dto.getTitulo());
        p.setPrecificacao(dto.getPrecificacao());
        return p;
    }
}