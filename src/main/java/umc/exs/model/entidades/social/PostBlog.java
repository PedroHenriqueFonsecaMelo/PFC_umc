package umc.exs.model.entidades.social;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import umc.exs.model.enums.StatusPost;

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

    @Builder.Default
    private int curtidas = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusPost status = StatusPost.RASCUNHO;

    /** Para agendamento de publicação futura (12.5.2). */
    private LocalDateTime dataPublicacaoAgendada;
}
