package umc.exs.dto.request.livro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejeicaoLivroRequest {
    @NotBlank(message = "O estado é obrigatório")
    private String estado;

    @NotBlank(message = "O comentário é obrigatório")
    @Size(max = 500, message = "O comentário não pode ter mais de 500 caracteres")
    private String comentario;
}
