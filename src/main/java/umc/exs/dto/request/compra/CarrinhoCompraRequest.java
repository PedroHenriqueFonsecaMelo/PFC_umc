package umc.exs.dto.request.compra;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO para compra em lote via carrinho.
 * Recebe lista de IDs de livros e, opcionalmente, um cupom de desconto aplicado
 * ao total.
 */
@Getter
@Setter
@NoArgsConstructor
public class CarrinhoCompraRequest {

    @NotEmpty(message = "O carrinho não pode estar vazio.")
    private List<Long> livroIds;

    /**
     * Código do cupom a ser aplicado no total da compra (opcional).
     */
    private String codigoCupom;
}
