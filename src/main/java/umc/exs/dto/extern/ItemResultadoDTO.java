package umc.exs.dto.extern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResultadoDTO {
    private Long livroId;
    private Long pedidoId;
    private String codigoPedido;
    private String titulo;
    private Double preco;
    private String motivo;
}
