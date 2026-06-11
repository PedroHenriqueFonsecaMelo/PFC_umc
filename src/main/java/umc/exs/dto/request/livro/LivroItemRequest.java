package umc.exs.dto.request.livro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa um livro individual dentro de um lote enviado pelo vendedor.
 * Contém os dados bibliográficos e a quantidade de fotos associadas ao exemplar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroItemRequest {

    // Título do livro; obrigatório, entre 3 e 255 caracteres
    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 255, message = "O título deve ter entre 3 e 255 caracteres")
    private String titulo;

    // Nome do autor do livro; obrigatório, entre 3 e 255 caracteres
    @NotBlank(message = "O autor é obrigatório")
    @Size(min = 3, max = 255, message = "O autor deve ter entre 3 e 255 caracteres")
    private String autor;

    // Código ISBN do livro; obrigatório, entre 10 e 20 caracteres
    @NotBlank(message = "O ISBN é obrigatório")
    @Size(min = 10, max = 20, message = "O ISBN deve ter entre 10 e 20 caracteres")
    private String isbn;

    // Idioma em que o livro está escrito; obrigatório
    @NotBlank(message = "O idioma é obrigatório")
    private String idioma;

    // Número de fotos enviadas para este livro no lote; não pode ser negativo
    @Min(value = 0, message = "A quantidade de fotos não pode ser negativa")
    private int quantidadedeFotos;
}
