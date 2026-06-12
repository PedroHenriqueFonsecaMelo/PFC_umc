package umc.exs.model.entidades.social;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;

/**
 * Representa um tópico do fórum de discussão da plataforma, com categoria,
 * respostas ordenadas por data e controle de visualizações.
 */
@Entity
@Table(name = "topico_forum")

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "autor", "respostas" })
public class TopicoForum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Título do tópico, máximo 200 caracteres.
    @Column(nullable = false, length = 200)
    private String titulo;

    // Texto completo do tópico.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    // Categoria do fórum (Resenhas, Dúvidas, Recomendações, Geral).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaForum categoria;

    // Cliente que criou o tópico.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Cliente autor;

    // Data e hora de criação, preenchida automaticamente.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    // Contador incrementado a cada acesso ao tópico.
    @Column(nullable = false)
    @Builder.Default
    private int visualizacoes = 0;

    // Contador de respostas publicadas no tópico.
    @Column(nullable = false)
    @Builder.Default
    private int qtdRespostas = 0;

    // True quando o autor marca o tópico como resolvido.
    @Column(nullable = false)
    @Builder.Default
    private boolean resolvido = false;

    // Lista de respostas ordenadas da mais antiga para a mais recente.
    @OneToMany(mappedBy = "topico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dataCriacao ASC")
    @Builder.Default
    private List<RespostaForum> respostas = new ArrayList<>();
}
