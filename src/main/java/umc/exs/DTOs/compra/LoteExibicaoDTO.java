package umc.exs.DTOs.compra;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoteExibicaoDTO {
    private Long id;
    private String codigoProtocolo;
    private String status;
    private LocalDateTime dataCriacao;

    public LoteExibicaoDTO(Long id, String codigoProtocolo, String status, LocalDateTime dataCriacao) {
        this.id = id;
        this.codigoProtocolo = codigoProtocolo;
        this.status = status;
        this.dataCriacao = dataCriacao;
    }
}
