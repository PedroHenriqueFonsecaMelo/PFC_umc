package umc.exs.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComentarioRequestDTO {
    private String isbn;
    private String titulo;
    private String autor;
    private String comentario;
    private Integer nota;
}