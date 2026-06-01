package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CriarCupomRequest {
    @NotBlank(message = "O código do cupom é obrigatório")
    @Size(min = 3, max = 50, message = "O código deve ter entre 3 e 50 caracteres")
    private String codigo;

    @NotNull(message = "O percentual de desconto é obrigatório")
    @Min(value = 1, message = "O desconto deve ser entre 1 e 100%")
    @Max(value = 100, message = "O desconto deve ser entre 1 e 100%")
    private Double percentualDesconto;

    @Min(value = 1, message = "A quantidade máxima deve ser maior que 0")
    private Integer quantidadeMaxima;

    private Long clienteId;

    @NotBlank(message = "A data de validade é obrigatória")
    private String dataValidade;
}
