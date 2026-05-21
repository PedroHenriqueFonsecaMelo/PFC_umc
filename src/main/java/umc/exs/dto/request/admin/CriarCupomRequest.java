package umc.exs.dto.request.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CriarCupomRequest {
    private String codigo;
    private Double percentualDesconto;
    private Integer quantidadeMaxima;
    private Long clienteId;
    private String dataValidade;
}
