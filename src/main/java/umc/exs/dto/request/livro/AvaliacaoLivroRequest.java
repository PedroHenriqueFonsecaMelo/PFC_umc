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
 * DTO for creating a book review
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvaliacaoLivroRequest {

    @NotBlank(message = "O ISBN é obrigatório")
    private String isbn;

    @NotBlank(message = "O título do livro é obrigatório")
    private String tituloLivro;

    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota deve ser entre 1 e 5")
    @Max(value = 5, message = "A nota deve ser entre 1 e 5")
    private Integer nota;

    @Size(max = 1000, message = "O comentário não pode ter mais de 1000 caracteres")
    private String comentario;
}
