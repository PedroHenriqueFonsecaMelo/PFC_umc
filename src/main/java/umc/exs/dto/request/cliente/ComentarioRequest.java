package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO enviado pelo cliente ao registrar uma avaliação e comentário sobre um livro na Central de Opinião.
 * O livro é identificado pelo ISBN, com título e autor preservados no momento da avaliação.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComentarioRequest {

    // Identificador do livro avaliado; obrigatório
    @NotBlank(message = "O ISBN é obrigatório")
    private String isbn;

    // Título do livro no momento da avaliação, preservado para exibição histórica
    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    // Autor do livro no momento da avaliação, preservado para exibição histórica
    @NotBlank(message = "O autor é obrigatório")
    private String autor;

    // Texto livre da avaliação escrita pelo cliente; opcional, máximo 1000 caracteres
    @Size(max = 1000, message = "O comentário não pode ter mais de 1000 caracteres")
    private String comentario;

    // Nota de 1 a 5 estrelas atribuída ao livro pelo cliente; obrigatória
    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota deve ser entre 1 e 5")
    @Max(value = 5, message = "A nota deve ser entre 1 e 5")
    private Integer nota;
}
