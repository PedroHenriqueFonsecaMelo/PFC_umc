package umc.exs.dto.request.livro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejeicaoLivroRequest {
    private String estado;
    private String comentario;
}
