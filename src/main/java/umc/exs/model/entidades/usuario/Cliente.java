package umc.exs.model.entidades.usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.github.manoelcampos.dtogen.DTO;
import umc.exs.converter.LocalDateStringConverter;
import umc.exs.converter.CpfConverter;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.enums.Genero;

@Entity
@Table(name = "users")
@DTO
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "cartoes", "enderecos", "senha" })
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String nome;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(nullable = false)
    private LocalDate datanasc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Genero gen;

    @Column(nullable = true)
    @Convert(converter = CpfConverter.class)
    private String cpf;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private int tentativas = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean bloqueada = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    @Builder.Default
    private Double saldoTokens = 0.0;

    @Column
    private String fotoPerfil;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Verificação de e-mail — false até o usuário clicar no link de confirmação.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerificado = false;

    /**
     * ID do endereço padrão selecionado para entrega. Null se nenhum selecionado.
     */
    @Column
    private Long enderecoSelecionadoId;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "cliente_cartao", joinColumns = @JoinColumn(name = "cliente_id"), inverseJoinColumns = @JoinColumn(name = "cartao_id"))
    @Builder.Default
    private Set<Cartao> cartoes = new HashSet<>();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "cliente_endereco", joinColumns = @JoinColumn(name = "cliente_id"), inverseJoinColumns = @JoinColumn(name = "endereco_id"))
    @Builder.Default
    private Set<Endereco> enderecos = new HashSet<>();

    @OneToMany(mappedBy = "avaliador")
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;

    // --- Métodos de Negócio (Encapsulamento) ---

    public void registrarFalhaLogin() {
        this.tentativas++;
        if (this.tentativas >= 5) {
            this.bloqueada = true;
        }
    }

    public void resetarTentativas() {
        this.tentativas = 0;
        this.bloqueada = false;
    }
}