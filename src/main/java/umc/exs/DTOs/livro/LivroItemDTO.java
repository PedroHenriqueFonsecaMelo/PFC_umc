package umc.exs.DTOs.livro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroItemDTO {
    private String titulo;
    private String autor;
    private String isbn;
    private int quantidadedeFotos;
}
