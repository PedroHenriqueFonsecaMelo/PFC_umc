package umc.exs.dto.request.livro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListaDesejosRequest {

    private String googleBookId;
    private String openLibraryWorkId;
    private String isbn;
    private String titulo;
    private String autor;
    
}
