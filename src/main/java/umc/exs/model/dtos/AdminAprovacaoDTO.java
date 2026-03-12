package umc.exs.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.foundation.enums.EstadoLivro;

/**
 * DTO para approval de livro pelo admin.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAprovacaoDTO {
    
    private EstadoLivro estadoAprovado;
    private String comentario;
}

