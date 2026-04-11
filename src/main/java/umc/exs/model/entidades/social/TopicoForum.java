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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;

@Entity
@Table(name = "topico_forum")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "autor", "respostas" })
public class TopicoForum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaForum categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Cliente autor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private int visualizacoes = 0;

    @Column(nullable = false)
    private int qtdRespostas = 0;

    @Column(nullable = false)
    private boolean resolvido = false;

    @OneToMany(mappedBy = "topico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dataCriacao ASC")
    private List<RespostaForum> respostas = new ArrayList<>();
}
