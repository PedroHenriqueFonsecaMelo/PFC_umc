package umc.exs.dto.request.livro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para receber os dados do formulário de cadastro de livro.
 * O usuário só fornece informações básicas sobre o livro.
 * Preço e estado são definidos exclusivamente pelo admin.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LivroRequest {

    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 255, message = "O título deve ter entre 3 e 255 caracteres")
    private String titulo;

    @NotBlank(message = "O autor é obrigatório")
    @Size(min = 3, max = 255, message = "O autor deve ter entre 3 e 255 caracteres")
    private String autor;

    @NotBlank(message = "O ISBN é obrigatório")
    @Size(min = 10, max = 20, message = "O ISBN deve ter entre 10 e 20 caracteres")
    private String isbn;
}
