package umc.exs.dto.response.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.dto.request.cliente.EnderecoShared;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import umc.exs.model.enums.StatusConta;

/**
 * DTO completo do perfil do cliente, usado no painel admin e na página de perfil do próprio cliente.
 * Agrupa dados pessoais, financeiros, histórico de compras, vendas e engajamento em uma única resposta.
 */
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
    private StatusConta statusConta;
    private LocalDateTime suspensaoAte;
    private String motivoSuspensao;
    private LocalDateTime dataAcao;
    private String adminAcao;
    private LocalDateTime emailNotificadoEm;
    private String nivel;
    private List<EnderecoShared> enderecos;

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

    /**
     * Resumo de um pedido exibido no histórico de compras do cliente.
     * Contém título, autor, preço, status de envio, data da compra e código de rastreio.
     */
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
