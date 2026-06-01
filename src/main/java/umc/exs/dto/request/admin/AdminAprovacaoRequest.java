package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;

/**
 * DTO para approval de livro pelo admin.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAprovacaoRequest {

    @NotNull(message = "O estado do livro é obrigatório")
    private EstadoLivro estadoAprovado;

    @Size(max = 500, message = "O comentário não pode ter mais de 500 caracteres")
    private String comentario;

    private String fotosUrls;

    @Min(value = 0, message = "O preço sugerido não pode ser negativo")
    private Double precoSugerido;

}
