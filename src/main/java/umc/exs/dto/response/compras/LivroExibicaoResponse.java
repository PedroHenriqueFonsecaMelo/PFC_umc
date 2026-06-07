package umc.exs.dto.response.compras;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LivroExibicaoResponse {
    private Long id;
    private String titulo;
    private String autor;
    private String isbn;
    private String fotoUrl;
    private String fotosUrls;
    private String descricao;
    private EstadoLivro estadoAprovado;
    private Double precoAprovado;
    private Boolean emPromocao;
    private Double precoOriginal;
    private LocalDateTime promocaoExpira;
}
