package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;

/**
 * DTO enviado pelo admin ao aprovar um livro pendente.
 * Define o estado de conservação e opcionalmente um comentário e preço sugerido;
 * o preço final é calculado automaticamente pelo sistema.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAprovacaoRequest {

    // Estado de conservação do livro (ex: NOVO, SEMINOVO, USADO); obrigatório
    @NotNull(message = "O estado do livro é obrigatório")
    private EstadoLivro estadoAprovado;

    // Comentário opcional do admin ao vendedor sobre a aprovação; máximo 500 caracteres
    @Size(max = 500, message = "O comentário não pode ter mais de 500 caracteres")
    private String comentario;

    // URLs das fotos do livro em formato JSON, atualizadas pelo admin se necessário
    private String fotosUrls;

    // Preço sugerido pelo admin; se informado, é usado como base para o cálculo do preço final
    @Positive(message = "A quantidade deve ser positiva")
    @Min(value = 0, message = "O preço sugerido não pode ser negativo")
    private Double precoSugerido;

}
