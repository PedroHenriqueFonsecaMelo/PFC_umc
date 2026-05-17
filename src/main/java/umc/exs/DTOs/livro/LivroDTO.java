package umc.exs.dtos.livro;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroDTO {

    private Long id;

    private String titulo;

    private String autor;

    private String isbn;

    private String fotosUrls;

    private String resumoOficial;

    private Boolean aprovado;

    private Double precoAprovado;

    private EstadoLivro estadoAprovado;

    private Boolean emPromocao;

    private Double precoOriginal;

    private LocalDateTime promocaoExpira;

    private String genero;
}