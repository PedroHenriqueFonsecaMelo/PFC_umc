package umc.exs.DTOs.compra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de resposta da compra do carrinho.
 * Informa quais livros foram comprados, quais falharam e o saldo restante.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrinhoCompraResponseDTO {

    private int totalSolicitados;
    private int totalComprados;
    private double totalGasto;
    private double saldoRestante;

    private List<ItemResultado> comprados;
    private List<ItemResultado> falhas;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResultado {
        private Long livroId;
        private String titulo;
        private Double preco;
        private String motivo; // preenchido apenas em falhas
    }
}
