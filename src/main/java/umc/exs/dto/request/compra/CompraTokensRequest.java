package umc.exs.dto.request.compra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * DTO usado na compra de tokens via PIX; o campo valor é enviado pelo cliente.
 * Os campos pixCopiaECola, qrCodeBase64 e pagamentoId são preenchidos pela strategy do Mercado Pago e retornados ao cliente com o QR Code e ID do pagamento.
 */
@Data
public class CompraTokensRequest {

    @NotNull(message = "O valor é obrigatório.")
    @Min(value = 1, message = "O valor mínimo para compra é R$ 1,00.")
    private Double valor;

    // Preenchido pelo servidor antes de chamar a strategy
    @JsonIgnore
    private String emailPagador;

    // Campos de resposta — preenchidos pela strategy e retornados ao cliente
    private String pixCopiaECola;
    private String qrCodeBase64;
    private String pagamentoId;
}