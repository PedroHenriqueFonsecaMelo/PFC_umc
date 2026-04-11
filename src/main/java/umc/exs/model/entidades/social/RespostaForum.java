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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import umc.exs.model.entidades.usuario.Cliente;

@Entity
@Table(name = "resposta_forum")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "autor", "topico", "curtidoresIds" })
public class RespostaForum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Cliente autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topico_id", nullable = false)
    private TopicoForum topico;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private boolean melhorResposta = false;

    @Column(nullable = false)
    private int qtdCurtidas = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "curtida_resposta", joinColumns = @JoinColumn(name = "resposta_id"))
    @Column(name = "cliente_id")
    private Set<Long> curtidoresIds = new HashSet<>();
}
