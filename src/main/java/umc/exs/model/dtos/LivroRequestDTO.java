package umc.exs.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.foundation.enums.EstadoLivro;

/**
 * DTO para receber os dados do formulário de cadastro de livro.
 * Observe que não usamos anotações do Jakarta Persistence aqui.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LivroRequestDTO {

    private String titulo;
    private String autor;
    private String isbn;
    private Double precoTokens;
    private EstadoLivro estado; 
    
}