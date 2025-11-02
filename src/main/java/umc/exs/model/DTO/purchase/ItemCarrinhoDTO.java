package umc.exs.model.DTO.purchase;

import java.math.BigDecimal;

import umc.exs.model.compras.ItemCarrinho;
import umc.exs.model.foundation.Produto;

public class ItemCarrinhoDTO {
    private Long produtoId;
    private String tituloProduto;
    private BigDecimal precificacao;
    private int quantidade;

    public ItemCarrinhoDTO() {
    }

    public static ItemCarrinhoDTO fromEntity(ItemCarrinho itemCarrinho) {
        ItemCarrinhoDTO dto = new ItemCarrinhoDTO();
        Produto produto = itemCarrinho.getProduto();
        dto.produtoId = produto.getId();  
        dto.tituloProduto = produto.getTitulo(); 
        dto.precificacao = produto.getPrecificacao();
        dto.quantidade = itemCarrinho.getQuantidade();
        return dto;
    }


    public ItemCarrinho toEntity() {
        Produto produto = new Produto();  
        produto.setId(this.produtoId);  
        produto.setTitulo(this.tituloProduto);  
        produto.setPrecificacao(this.precificacao);  
        
        ItemCarrinho itemCarrinho = new ItemCarrinho();
        itemCarrinho.setProduto(produto);  
        itemCarrinho.setQuantidade(this.quantidade);  
        return itemCarrinho;
    }

    // Getters e Setters
    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getTituloProduto() {
        return tituloProduto;
    }

    public void setTituloProduto(String tituloProduto) {
        this.tituloProduto = tituloProduto;
    }

    public BigDecimal getPrecificacao() {
        return precificacao;
    }

    public void setPrecificacao(BigDecimal precificacao) {
        this.precificacao = precificacao;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}
