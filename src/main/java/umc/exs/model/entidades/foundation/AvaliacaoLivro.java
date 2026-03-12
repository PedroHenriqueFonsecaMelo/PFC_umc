package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.entidades.usuario.Cliente;

/**
 * Entity for book reviews and story information.
 * This represents reviews about the STORY of the book (like Harry Potter),
 * not about the physical book being sold.
 */
@Entity
@Builder
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoLivro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The book's ISBN - used to group reviews for the same book title
    private String isbn;

    // Title of the book (for display purposes)
    private String tituloLivro;

    // Rating from 1 to 5
    private Integer nota;

    // Review text
    private String comentario;

    // Review date
    private LocalDateTime dataAvaliacao;

    // Who wrote the review
    @ManyToOne
    private Cliente avaliador;
}

