package umc.exs.model.daos.mappers;

import umc.exs.model.dtos.admin.ProdutoDTO;
import umc.exs.model.entidades.foundation.Produto;

public class ProdutoMapper {
    public static ProdutoDTO fromEntity(Produto p) {
        if (p == null) return null;
        ProdutoDTO dto = new ProdutoDTO();
        dto.setId(p.getId());
        dto.setTitulo(p.getTitulo());
        dto.setPrecificacao(p.getPrecificacao());
        dto.setDescricaoDoProduto(p.getDescricaoDoProduto());
        return dto;
    }

    public static Produto toEntity(ProdutoDTO dto) {
        if (dto == null) return null;
        Produto p = new Produto();
        p.setId(dto.getId());
        p.setTitulo(dto.getTitulo());
        p.setPrecificacao(dto.getPrecificacao());
        p.setDescricaoDoProduto(dto.getDescricaoDoProduto());
        return p;
    }
}