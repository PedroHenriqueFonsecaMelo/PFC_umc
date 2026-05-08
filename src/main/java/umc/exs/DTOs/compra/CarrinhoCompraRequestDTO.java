package umc.exs.DTOs.compra;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO para compra em lote via carrinho.
 * Recebe lista de IDs de livros e, opcionalmente, cupons aplicados por item.
 */
@Getter
@Setter
@NoArgsConstructor
public class CarrinhoCompraRequestDTO {

    @NotEmpty(message = "O carrinho não pode estar vazio.")
    private List<Long> livroIds;

    /**
     * Cupons aplicados a itens específicos do carrinho (opcional).
     * Cada entrada associa um livroId a um código de cupom.
     */
    private List<CupomAplicado> cupons;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CupomAplicado {
        private Long livroId;
        private String codigo;
    }
}
