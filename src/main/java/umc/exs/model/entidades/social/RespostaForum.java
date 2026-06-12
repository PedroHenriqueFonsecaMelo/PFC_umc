package umc.exs.model.entidades.social;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import umc.exs.model.entidades.usuario.Cliente;

/**
 * Representa uma resposta a um tópico do fórum, com suporte a curtidas,
 * marcação de melhor resposta e controle de autoria.
 */
@Entity
@Table(name = "resposta_forum")

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "autor", "topico", "curtidoresIds" })
public class RespostaForum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Texto da resposta.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    // Cliente que escreveu a resposta.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Cliente autor;

    // Tópico ao qual a resposta pertence.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topico_id", nullable = false)
    private TopicoForum topico;

    // Data e hora de criação, preenchida automaticamente.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    // True quando marcada como melhor pelo autor do tópico ou admin.
    @Column(nullable = false)
    @Builder.Default
    private boolean melhorResposta = false;

    // Contador de curtidas da resposta.
    @Column(nullable = false)
    @Builder.Default
    private int qtdCurtidas = 0;

    // Conjunto de IDs dos clientes que curtiram a resposta.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "curtida_resposta", joinColumns = @JoinColumn(name = "resposta_id"))
    @Column(name = "cliente_id")
    @Builder.Default
    private Set<Long> curtidoresIds = new HashSet<>();
}
