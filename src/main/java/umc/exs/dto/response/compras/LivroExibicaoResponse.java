package umc.exs.dto.response.compras;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;
import java.time.LocalDateTime;

/**
 * DTO de resposta usado para exibir livros na vitrine pública.
 * Contém dados de preço, estado de conservação, fotos e informações de promoção quando aplicável.
 */
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
