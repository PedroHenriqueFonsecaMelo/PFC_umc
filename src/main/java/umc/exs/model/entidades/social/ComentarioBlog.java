package umc.exs.model.entidades.social;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comentario_blog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComentarioBlog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostBlog post;

    private String autorNome;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Builder.Default
    private LocalDateTime dataCriacao = LocalDateTime.now();
}
