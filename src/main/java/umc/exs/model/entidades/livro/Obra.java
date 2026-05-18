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
import io.github.manoelcampos.dtogen.DTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DTO
@Entity
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo; // Ex: "The Hobbit"

    private String autor;

    private String idioma;

    @Column(columnDefinition = "TEXT")
    private String imageLinksJson;

    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<Livro> edicoes;

    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;

}
