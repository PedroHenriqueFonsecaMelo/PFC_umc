package umc.exs.DTOs.livro;

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
public class LivroRequestDTO {

    private String titulo;
    private String autor;
    private String isbn;
}
