package umc.exs.dto.extern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
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
