package umc.exs.dto.request.livro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO enviado pelo cliente ao registrar uma avaliação de livro pelo fluxo legado,
 * com ISBN, título, nota de 1 a 5 e comentário opcional.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvaliacaoLivroRequest {

    // Identificador do livro avaliado; obrigatório
    @NotBlank(message = "O ISBN é obrigatório")
    private String isbn;

    // Título do livro no momento da avaliação; obrigatório
    @NotBlank(message = "O título do livro é obrigatório")
    private String tituloLivro;

    // Nota de 1 a 5 estrelas atribuída ao livro pelo cliente; obrigatória
    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota deve ser entre 1 e 5")
    @Max(value = 5, message = "A nota deve ser entre 1 e 5")
    private Integer nota;

    // Texto livre da avaliação escrita pelo cliente; opcional, máximo 1000 caracteres
    @Size(max = 1000, message = "O comentário não pode ter mais de 1000 caracteres")
    private String comentario;
}
