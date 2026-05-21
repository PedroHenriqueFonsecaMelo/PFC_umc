package umc.exs.dto.request.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;

/**
 * DTO para approval de livro pelo admin.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAprovacaoRequest {

    private EstadoLivro estadoAprovado;
    private String comentario;
    private String fotosUrls;
    private Double precoSugerido;

}
