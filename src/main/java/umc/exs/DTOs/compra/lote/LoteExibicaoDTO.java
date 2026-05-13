package umc.exs.DTOs.compra.lote;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoteExibicaoDTO {
    private Long id;
    private String codigoProtocolo;
    private String status;
    private LocalDateTime dataCriacao;
    private String nomeVendedor;
    private String emailVendedor;
    private long quantidadeLivros;

    /** Construtor legado (compatibilidade com código existente) */
    public LoteExibicaoDTO(Long id, String codigoProtocolo, String status, LocalDateTime dataCriacao) {
        this.id = id;
        this.codigoProtocolo = codigoProtocolo;
        this.status = status;
        this.dataCriacao = dataCriacao;
    }

    public LoteExibicaoDTO(Long id, String codigoProtocolo, String status, LocalDateTime dataCriacao,
                           String nomeVendedor, String emailVendedor, long quantidadeLivros) {
        this.id = id;
        this.codigoProtocolo = codigoProtocolo;
        this.status = status;
        this.dataCriacao = dataCriacao;
        this.nomeVendedor = nomeVendedor;
        this.emailVendedor = emailVendedor;
        this.quantidadeLivros = quantidadeLivros;
    }
}
