package umc.exs.DTOs.compra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompraTokensRequestDTO {

    @NotNull(message = "O valor é obrigatório.")
    @Min(value = 1, message = "O valor mínimo para compra é R$ 1,00.")
    private Double valor;

    @NotBlank(message = "O método de pagamento deve ser informado.")
    private String metodoPagamento;

    private Long clienteId;

    // Dados de entrada (Cartão)
    private String numeroCartao;
    private String nomeCartao;
    private String cvv;
    private String validade;

    // --- NOVOS CAMPOS PARA RESPOSTA (PIX) ---
    private String pixCopiaECola;
    private String qrCodeBase64;
    private String pagamentoId;
}