package umc.exs.DTOs.compra;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CupomDTO {

    private Long id;
    private String codigo;
    private Double valorTokens;
    private LocalDateTime expiracao;
    private boolean usado;
    private String tipo;
    private LocalDateTime dataCriacao;
}
