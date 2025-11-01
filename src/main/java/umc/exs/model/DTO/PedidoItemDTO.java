package umc.exs.model.DTO;

import umc.exs.model.compras.PedidoItem;

public class PedidoItemDTO {
    private Long id;
    private Long produtoId;
    private String produtoTitulo;
    private Integer quantidade;
    private Double precoUnitario;

    public PedidoItemDTO() {
    }

    // Converte uma entidade PedidoItem em um PedidoItemDTO
    public static PedidoItemDTO fromEntity(PedidoItem it) {
        if (it == null)
            return null;
        PedidoItemDTO dto = new PedidoItemDTO();
        dto.id = it.getId();
        dto.produtoId = it.getProdutoId();
        dto.produtoTitulo = it.getProdutoTitulo();
        dto.quantidade = it.getQuantidade();
        dto.precoUnitario = it.getPrecoUnitario();
        return dto;
    }

    // Converte um PedidoItemDTO de volta para a entidade PedidoItem
    public PedidoItem toEntity() {
        PedidoItem it = new PedidoItem();
        it.setId(this.id);
        it.setProdutoId(this.produtoId);
        it.setProdutoTitulo(this.produtoTitulo);
        it.setQuantidade(this.quantidade);
        it.setPrecoUnitario(this.precoUnitario);
        return it;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoTitulo() {
        return produtoTitulo;
    }

    public void setProdutoTitulo(String produtoTitulo) {
        this.produtoTitulo = produtoTitulo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(Double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
