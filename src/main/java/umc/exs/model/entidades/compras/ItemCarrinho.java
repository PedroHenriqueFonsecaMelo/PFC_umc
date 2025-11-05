package umc.exs.model.entidades.compras;

import java.math.BigDecimal;

import umc.exs.model.entidades.foundation.Produto;

public class ItemCarrinho {
    private Produto produto;
    private int quantidade;

    public ItemCarrinho(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public ItemCarrinho() {
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal calcularSubtotal() {

        return this.getProduto().getPrecificacao().multiply(new BigDecimal(this.getQuantidade()));
    }
}
