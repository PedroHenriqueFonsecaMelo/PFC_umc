package umc.exs.model.entidades.livro;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import umc.exs.model.entidades.usuario.Cliente;

/**
 * Representa uma avaliação e comentário de um livro feito por um cliente,
 * vinculado à obra canônica e preservando título e autor no momento da
 * avaliação.
 */
@Entity

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoLivro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Obra literária avaliada.
    @ManyToOne
    @JoinColumn(name = "obra_id", nullable = false)
    private Obra obra;

    // ISBN da edição específica no momento da avaliação.
    private String isbnOriginalNoAto;

    // Texto da avaliação escrito pelo cliente.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String comentario;

    // Nota de 1 a 5 estrelas.
    private Integer nota;

    // Data e hora em que a avaliação foi registrada.
    private LocalDateTime dataAvaliacao;

    // Cliente que escreveu a avaliação.
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente avaliador;

    // Título preservado no momento da avaliação.
    private String tituloLivro;

    // Autor preservado no momento da avaliação.
    private String autorLivro;
}