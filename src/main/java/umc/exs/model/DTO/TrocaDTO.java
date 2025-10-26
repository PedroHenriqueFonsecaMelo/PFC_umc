package umc.exs.model.DTO;

import java.time.LocalDateTime;

import umc.exs.model.compras.Troca;

public class TrocaDTO {
    private Long id;
    private Long pedidoId;
    private Long clienteId;
    private Double valor;
    private String status;
    private String motivoRejeicao;
    private String decisaoPor;
    private LocalDateTime decisionAt;
    private LocalDateTime createdAt;

    public TrocaDTO() {
    }

    public static TrocaDTO fromEntity(Troca t) {
        if (t == null)
            return null;
        TrocaDTO dto = new TrocaDTO();
        dto.id = t.getId();
        dto.pedidoId = t.getPedidoId();
        dto.clienteId = t.getClienteId();
        dto.valor = t.getValor();
        dto.status = t.getStatus();
        dto.motivoRejeicao = t.getMotivoRejeicao();
        dto.decisaoPor = t.getDecisaoPor();
        dto.decisionAt = t.getDecisionAt();
        dto.createdAt = t.getCreatedAt();
        return dto;
    }

    public Troca toEntity() {
        Troca t = new Troca();
        t.setId(this.id);
        t.setPedidoId(this.pedidoId);
        t.setClienteId(this.clienteId);
        t.setValor(this.valor);
        t.setStatus(this.status);
        t.setMotivoRejeicao(this.motivoRejeicao);
        t.setDecisaoPor(this.decisaoPor);
        t.setDecisionAt(this.decisionAt);
        t.setCreatedAt(this.createdAt);
        return t;
    }

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public String getDecisaoPor() {
        return decisaoPor;
    }

    public void setDecisaoPor(String decisaoPor) {
        this.decisaoPor = decisaoPor;
    }

    public LocalDateTime getDecisionAt() {
        return decisionAt;
    }

    public void setDecisionAt(LocalDateTime decisionAt) {
        this.decisionAt = decisionAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
