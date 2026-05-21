package umc.exs.dto.response.compras;

import java.time.LocalDateTime;

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
public class CupomResponse {

    private Long id;
    private String codigo;
    private Double percentualDesconto;
    private LocalDateTime expiracao;
    private boolean usado;
    private String tipo;
    private LocalDateTime dataCriacao;
    private Integer quantidadeMaxima;
    private Integer quantidadeUsada;

    private String clienteNome;
    private String clienteEmail;
}