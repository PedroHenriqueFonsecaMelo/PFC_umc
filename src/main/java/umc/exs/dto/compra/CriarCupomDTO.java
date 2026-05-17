package umc.exs.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CriarCupomDTO {
    private String codigo;
    private Double percentualDesconto;
    private Integer quantidadeMaxima;
    private Long clienteId;
    private String dataValidade;
}
