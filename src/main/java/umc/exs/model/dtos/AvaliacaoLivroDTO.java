package umc.exs.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a book review
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvaliacaoLivroDTO {

    private String isbn;
    private String tituloLivro;
    private Integer nota; // 1 to 5
    private String comentario;
}

