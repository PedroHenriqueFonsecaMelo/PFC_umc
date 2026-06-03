package umc.exs.dto.response.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.usuario.Endereco;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientePerfilResponse {

    // Dados pessoais
    private Long id;
    private String nome;
    private String email;
    private String fotoPerfil;
    private String cpfMascarado;
    private LocalDate dataNascimento;
    private LocalDateTime dataCadastro;
    private boolean ativo;
    private String nivel;
    private List<Endereco> enderecos;

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
