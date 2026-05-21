package umc.exs.dto.request.livro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroItemRequest {

    private String titulo;
    private String autor;
    private String isbn;
    private String idioma;
    private int quantidadedeFotos;
}
