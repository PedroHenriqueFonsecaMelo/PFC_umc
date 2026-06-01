package umc.exs.dto.request.livro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroItemRequest {

    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 255, message = "O título deve ter entre 3 e 255 caracteres")
    private String titulo;

    @NotBlank(message = "O autor é obrigatório")
    @Size(min = 3, max = 255, message = "O autor deve ter entre 3 e 255 caracteres")
    private String autor;

    @NotBlank(message = "O ISBN é obrigatório")
    @Size(min = 10, max = 20, message = "O ISBN deve ter entre 10 e 20 caracteres")
    private String isbn;

    @NotBlank(message = "O idioma é obrigatório")
    private String idioma;

    @Min(value = 0, message = "A quantidade de fotos não pode ser negativa")
    private int quantidadedeFotos;
}
