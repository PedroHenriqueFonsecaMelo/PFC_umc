package umc.exs.model.DTO.purchase;

import java.math.BigDecimal;

public class PagamentoDTO {
    private Long cartaoId;
    private BigDecimal valor;

    public Long getCartaoId() {
        return cartaoId;
    }

    public void setCartaoId(Long cartaoId) {
        this.cartaoId = cartaoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public void setValor(float total) {
        this.valor = BigDecimal.valueOf(total);
    }
}
