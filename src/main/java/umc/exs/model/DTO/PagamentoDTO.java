package umc.exs.model.DTO;

public class PagamentoDTO {
    private Long cartaoId;
    private float valor;
    
    public Long getCartaoId() {
        return cartaoId;
    }
    public void setCartaoId(Long cartaoId) {
        this.cartaoId = cartaoId;
    }
    public float getValor() {
        return valor;
    }
    public void setValor(float valor) {
        this.valor = valor;
    }
}
