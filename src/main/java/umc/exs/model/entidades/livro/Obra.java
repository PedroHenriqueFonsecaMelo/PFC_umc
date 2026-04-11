package umc.exs.model.entidades.livro;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tituloOriginal; // Ex: "The Hobbit"
    private String autor;

    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<Livro> edicoes;

    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;
}
