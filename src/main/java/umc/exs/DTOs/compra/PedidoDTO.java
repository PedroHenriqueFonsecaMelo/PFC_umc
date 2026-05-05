package umc.exs.DTOs.compra;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.StatusEnvio;

/**
 * DTO de exibição de um pedido para o cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {

    private Long id;
    private Long livroId;
    private String tituloLivro;
    private String autorLivro;
    private String isbnLivro;
    private String fotosUrls;
    private Double precoLivro;
    private StatusEnvio statusEnvio;
    private String statusEnvioDescricao;
    private LocalDateTime dataCompra;
    private LocalDateTime dataAtualizacaoStatus;
    private String codigoRastreio;

    // Dados do comprador (populados para a visão admin)
    private String compradorNome;
    private String compradorEmail;
    private String compradorEndereco;

    // Preenchido apenas quando o pedido é cancelado com estorno
    private Double saldoAposEstorno;
}
