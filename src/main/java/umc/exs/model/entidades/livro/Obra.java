package umc.exs.model.entidades.livro;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Representa a obra literária canônica (título + autor), agrupando todas as
 * edições físicas e avaliações do mesmo livro independente do ISBN da edição.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo; // Ex: "The Hobbit"

    // Autor da obra.
    private String autor;

    // Idioma original da obra.
    private String idioma;

    // URLs das capas em formato JSON da API Google Books.
    @Column(columnDefinition = "TEXT")
    private String imageLinksJson;

    // Lista de exemplares físicos desta obra disponíveis na plataforma.
    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<Livro> edicoes;

    // Lista de avaliações da comunidade para esta obra.
    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;

}
