package umc.exs.dto.extern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Representa o resultado individual de cada livro processado em uma compra de carrinho.
 * Indica sucesso (pedidoId preenchido) ou falha (motivo preenchido) para cada item.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResultadoDTO {

    /** ID do livro processado neste item da compra. */
    private Long livroId;

    /** ID do pedido gerado em caso de sucesso; null se o item falhou. */
    private Long pedidoId;

    /** Código do pedido no formato BIB-YYYYMMDD-XXXX, compartilhado entre os itens da mesma compra. */
    private String codigoPedido;

    /** Título do livro no momento da compra, preservado para exibição no resultado. */
    private String titulo;

    /** Preço cobrado pelo livro no momento da compra. */
    private Double preco;

    /** Motivo da falha no processamento do item; null se a compra foi bem-sucedida. */
    private String motivo;
}
