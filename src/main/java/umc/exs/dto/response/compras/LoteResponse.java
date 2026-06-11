package umc.exs.dto.response.compras;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta de um lote de livros submetido pelo vendedor.
 * Contém código de protocolo, status, dados do vendedor e quantidade de livros no lote.
 */
@Data
@NoArgsConstructor
public class LoteResponse {
    private Long id;
    private String codigoProtocolo;
    private String status;
    private LocalDateTime dataCriacao;
    private String nomeVendedor;
    private String emailVendedor;
    private long quantidadeLivros;

    /** Construtor legado (compatibilidade com código existente) */
    public LoteResponse(Long id, String codigoProtocolo, String status, LocalDateTime dataCriacao) {
        this.id = id;
        this.codigoProtocolo = codigoProtocolo;
        this.status = status;
        this.dataCriacao = dataCriacao;
    }

    /**
     * Inicializa todos os campos do lote, incluindo nome e e-mail do vendedor e quantidade de livros.
     * Utilizado quando os dados completos do vendedor estão disponíveis para exibição no painel admin.
     */
    public LoteResponse(Long id, String codigoProtocolo, String status, LocalDateTime dataCriacao,
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
