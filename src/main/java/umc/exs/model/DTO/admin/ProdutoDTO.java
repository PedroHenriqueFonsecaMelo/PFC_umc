package umc.exs.model.DTO.admin;

import java.math.BigDecimal;

import umc.exs.model.foundation.Produto;

public class ProdutoDTO {
    private Long id;
    private String titulo;
    private BigDecimal precificacao;
    private String descricaoDoProduto;  // Alterado para camelCase

    public ProdutoDTO() {
    }

    // Conversão de Entidade para DTO
    public static ProdutoDTO fromEntity(Produto p) {
        if (p == null)
            return null;
        ProdutoDTO dto = new ProdutoDTO();
        dto.id = p.getId();
        dto.titulo = p.getTitulo();
        dto.precificacao = p.getPrecificacao();
        dto.descricaoDoProduto = p.getDescricaoDoProduto();  // Consistência no nome do campo
        return dto;
    }

    // Conversão de DTO para Entidade
    public Produto toEntity() {
        Produto p = new Produto();
        p.setId(this.id);
        p.setTitulo(this.titulo);
        p.setPrecificacao(this.precificacao);
        p.setDescricaoDoProduto(this.descricaoDoProduto);  // Consistência no nome do campo
        return p;
    }

    // Getters e Setters
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

    public BigDecimal getPrecificacao() {
        return precificacao;
    }

    public void setPrecificacao(BigDecimal precificacao) {
        this.precificacao = precificacao;
    }

    public String getDescricaoDoProduto() {
        return descricaoDoProduto;
    }

    public void setDescricaoDoProduto(String descricaoDoProduto) {
        this.descricaoDoProduto = descricaoDoProduto;
    }

    @Override
    public String toString() {
        return "ProdutoDTO [id=" + id + ", titulo=" + titulo + ", precificacao=" + precificacao + ", descricaoDoProduto="
                + descricaoDoProduto + "]";
    }

    public void setPrecificacao(float f) {
        this.precificacao = BigDecimal.valueOf(f);
    }
}
