package umc.exs.dto.response.compras;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;


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
}
