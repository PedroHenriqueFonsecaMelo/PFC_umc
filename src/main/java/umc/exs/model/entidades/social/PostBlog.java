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

/**
 * Representa um post do blog da plataforma, com suporte a rascunho,
 * publicação imediata e agendamento para data futura.
 */
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

    // Título do post.
    private String titulo;

    // Corpo do post em texto ou HTML.
    @Column(columnDefinition = "TEXT")
    private String conteudo;

    // URL da imagem de capa do post.
    private String imagemUrl;

    // Nome do admin que criou o post.
    private String autorNome;

    // Data e hora de publicação, preenchida automaticamente.
    @Builder.Default
    private LocalDateTime dataPublicacao = LocalDateTime.now();

    // Contador de curtidas dos usuários.
    @Builder.Default
    private int curtidas = 0;

    // Estado do post: RASCUNHO, EM_REVISAO, AGENDADO ou PUBLICADO.
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusPost status = StatusPost.RASCUNHO;

    /** Para agendamento de publicação futura (12.5.2). */
    private LocalDateTime dataPublicacaoAgendada;
}
