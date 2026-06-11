package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO usado pelo admin para criar um novo cupom de desconto.
 * Define código, percentual, limite de uso, cliente específico e data de validade.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CriarCupomRequest {

    // Código único do cupom informado pelo admin (ex: PROMO10); entre 3 e 50 caracteres
    @NotBlank(message = "O código do cupom é obrigatório")
    @Size(min = 3, max = 50, message = "O código deve ter entre 3 e 50 caracteres")
    private String codigo;

    // Percentual de desconto aplicado ao valor da compra; deve estar entre 1 e 100%
    @NotNull(message = "O percentual de desconto é obrigatório")
    @Min(value = 1, message = "O desconto deve ser entre 1 e 100%")
    @Max(value = 100, message = "O desconto deve ser entre 1 e 100%")
    private Double percentualDesconto;

    // Limite de vezes que o cupom pode ser utilizado no total; opcional
    @Min(value = 1, message = "A quantidade máxima deve ser maior que 0")
    private Integer quantidadeMaxima;

    // Se informado, restringe o uso do cupom a um cliente específico
    private Long clienteId;

    // Data e hora de expiração do cupom no formato ISO-8601 (ex: 2025-12-31T23:59:59)
    @NotBlank(message = "A data de validade é obrigatória")
    private String dataValidade;
}
