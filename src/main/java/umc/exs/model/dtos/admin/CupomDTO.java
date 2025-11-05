package umc.exs.model.dtos.admin;

import java.time.LocalDateTime;

import umc.exs.model.entidades.compras.Cupom;

public class CupomDTO {
    private Long id;
    private String codigo;
    private Float valor;
    private Long clienteId;
    private LocalDateTime expiracao;
    private boolean usado;

    public CupomDTO() {
    }

    public static CupomDTO fromEntity(Cupom c) {
        if (c == null)
            return null;
        CupomDTO dto = new CupomDTO();
        dto.id = c.getId();
        dto.codigo = c.getCodigo();
        dto.valor = c.getValor();
        dto.clienteId = c.getClienteId();
        dto.expiracao = c.getExpiracao();
        dto.usado = c.isUsado();
        return dto;
    }

    public Cupom toEntity() {
        Cupom c = new Cupom();
        c.setId(this.id);
        c.setCodigo(this.codigo);
        c.setValor(this.valor);
        c.setClienteId(this.clienteId);
        c.setExpiracao(this.expiracao);
        c.setUsado(this.usado);
        return c;
    }

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Float getValor() {
        return valor;
    }

    public void setValor(Float valor) {
        this.valor = valor;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public LocalDateTime getExpiracao() {
        return expiracao;
    }

    public void setExpiracao(LocalDateTime expiracao) {
        this.expiracao = expiracao;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }
}
