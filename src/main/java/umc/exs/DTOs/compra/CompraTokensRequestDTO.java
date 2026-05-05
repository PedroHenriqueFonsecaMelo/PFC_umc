package umc.exs.DTOs.compra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class CompraTokensRequestDTO {

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