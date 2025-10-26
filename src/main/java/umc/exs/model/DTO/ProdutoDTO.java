package umc.exs.model.DTO;

import umc.exs.model.foundation.Produto;

public class ProdutoDTO {
    private Long id;
    private String titulo;
    private Float precificacao;

    public ProdutoDTO() {
    }

    public static ProdutoDTO fromEntity(Produto p) {
        if (p == null)
            return null;
        ProdutoDTO dto = new ProdutoDTO();
        dto.id = p.getId();
        dto.titulo = p.getTitulo();
        dto.precificacao = p.getPrecificacao();
        return dto;
    }

    public Produto toEntity() {
        Produto p = new Produto();
        p.setId(this.id);
        p.setTitulo(this.titulo);
        p.setPrecificacao(this.precificacao);
        return p;
    }

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Float getPrecificacao() {
        return precificacao;
    }

    public void setPrecificacao(Float precificacao) {
        this.precificacao = precificacao;
    }
}
