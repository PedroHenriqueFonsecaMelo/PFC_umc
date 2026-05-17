package umc.exs.dto.compra;

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
    private String titulo;
    private Double preco;
    private String motivo;
}
