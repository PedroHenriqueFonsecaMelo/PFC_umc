package umc.exs.dto.request.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComentarioRequest {
    private String isbn;
    private String titulo;
    private String autor;
    private String comentario;
    private Integer nota;
}