package umc.exs.model.entidades.livro;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import umc.exs.model.entidades.usuario.Cliente;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoLivro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "obra_id", nullable = false)
    private Obra obra;

    private String isbnOriginalNoAto;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comentario;

    private Integer nota;
    
    private LocalDateTime dataAvaliacao;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente avaliador;

    private String tituloLivro;

    private String autorLivro;
}