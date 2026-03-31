package umc.exs.DTOs.compra;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO para compra em lote via carrinho.
 * Recebe lista de IDs de livros a comprar numa única transação.
 */
@Getter
@Setter
@NoArgsConstructor
public class CarrinhoCompraRequestDTO {

    @NotEmpty(message = "O carrinho não pode estar vazio.")
    private List<Long> livroIds;
}
