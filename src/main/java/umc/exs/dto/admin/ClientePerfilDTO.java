package umc.exs.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientePerfilDTO {

    // Dados pessoais
    private Long id;
    private String nome;
    private String email;
    private String cpfMascarado;
    private String dataNascimento;
    private LocalDateTime dataCadastro;
    private boolean ativo;
    private String nivel;

    // Resumo financeiro
    private Double saldoTokens;
    private Double totalGasto;
    private Double totalRecarregado;
    private long quantidadeCuponsUsados;

    // Compras
    private long totalPedidos;
    private long totalCancelamentos;
    private List<PedidoResumoDTO> pedidos;

    // Vendas
    private long totalLivrosVendidos;
    private long totalLotesEnviados;
    private long totalLivrosRejeitados;

    // Engajamento
    private long totalTopicosForum;
    private long totalListaDesejos;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PedidoResumoDTO {
        private Long id;
        private String titulo;
        private String autor;
        private Double preco;
        private String status;
        private LocalDateTime dataCompra;
        private String codigoRastreio;
    }
}
