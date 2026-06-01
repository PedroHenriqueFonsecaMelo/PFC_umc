package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComentarioRequest {
    @NotBlank(message = "O ISBN é obrigatório")
    private String isbn;

    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    @NotBlank(message = "O autor é obrigatório")
    private String autor;

    @Size(max = 1000, message = "O comentário não pode ter mais de 1000 caracteres")
    private String comentario;

    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota deve ser entre 1 e 5")
    @Max(value = 5, message = "A nota deve ser entre 1 e 5")
    private Integer nota;
}