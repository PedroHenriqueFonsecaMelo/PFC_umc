package umc.exs.dtos.compra.cupom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CupomValidacaoDTO {

    private boolean valido;
    private Double percentual;
    private Double precoOriginal;
    private Double precoComDesconto;
    private Double economia;
    private String mensagem;
}