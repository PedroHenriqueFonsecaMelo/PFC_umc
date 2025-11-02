package umc.exs.model.DTO.purchase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import umc.exs.model.compras.Pedido;

public class PedidoDTO {
    private Long id;
    private Long clienteId;
    private Double total;
    private LocalDateTime data;
    private String status;
    private Long enderecoId;
    private List<PedidoItemDTO> itens;

    public PedidoDTO() {
    }

    public static PedidoDTO fromEntity(Pedido p) {
        if (p == null)
            return null;
        PedidoDTO dto = new PedidoDTO();
        dto.id = p.getId();
        dto.clienteId = p.getClienteId();
        dto.total = p.getTotal();
        dto.data = p.getData();
        dto.status = p.getStatus();
        dto.enderecoId = p.getEnderecoId();
        dto.itens = p.getItens() == null ? null
                : p.getItens().stream().map(PedidoItemDTO::fromEntity).collect(Collectors.toList());
        return dto;
    }
    
    public Pedido toEntity() {
        Pedido p = new Pedido();
        p.setId(this.id);
        p.setClienteId(this.clienteId);
        p.setTotal(this.total);
        p.setData(this.data);
        p.setStatus(this.status);
        p.setEnderecoId(this.enderecoId);

        // Correção aqui, mapeando explicitamente com lambda
        p.setItens(this.itens == null ? null
                : this.itens.stream().map(item -> item.toEntity()).collect(Collectors.toList()));

        return p;
    }

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getEnderecoId() {
        return enderecoId;
    }

    public void setEnderecoId(Long enderecoId) {
        this.enderecoId = enderecoId;
    }

    public List<PedidoItemDTO> getItens() {
        return itens;
    }

    public void setItens(List<PedidoItemDTO> itens) {
        this.itens = itens;
    }
}
