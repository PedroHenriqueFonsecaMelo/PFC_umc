package umc.exs.dto.livro;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.enums.StatusVenda;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinhaVendaDTO {

    private Long id;
    private String titulo;
    private String autor;
    private String isbn;
    private EstadoLivro estadoAprovado;
    private Double precoAprovado;
    /** Primeira foto do array JSON fotosUrls */
    private String primeiraFoto;
    private LocalDateTime dataAnuncio;
    private StatusVenda statusVenda;
    private String motivoRejeicao;
}
