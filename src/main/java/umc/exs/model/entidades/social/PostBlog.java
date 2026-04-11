package umc.exs.model.entidades.social;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_blog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostBlog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    private String imagemUrl;

    private String autorNome;

    @Builder.Default
    private LocalDateTime dataPublicacao = LocalDateTime.now();
}
