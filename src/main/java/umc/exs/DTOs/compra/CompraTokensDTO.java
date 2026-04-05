package umc.exs.DTOs.compra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para recarga de tokens na carteira.
 * Validações: valor mínimo R$1, método válido, cartão formato correto.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraTokensDTO {

    @NotNull(message = "Valor é obrigatório")
    @Min(value = 1, message = "Valor mínimo para recarga é R$ 1,00")
    private Double valor;

    @NotBlank(message = "Método de pagamento deve ser informado")
    private String metodoPagamento;

    @Pattern(regexp = "^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14})$", message = "Número do cartão inválido (aceita Visa/Mastercard)")
    private String numCartao;

}
