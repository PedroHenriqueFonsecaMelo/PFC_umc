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

import umc.exs.config.LocalDateTimeConverter;
import umc.exs.converter.LocalDateStringConverter;
import umc.exs.converter.CpfConverter;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.enums.Genero;
import umc.exs.model.enums.StatusConta;

/**
 * Representa o cliente da plataforma, com dados pessoais, saldo de tokens,
 * controle de acesso e relacionamentos com endereços, cartões e avaliações.
 */
@Entity
@Table(name = "users")

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

    // Nome do cliente.
    @Column(nullable = false)
    private String nome;

    // Senha criptografada para autenticação.
    @Column(nullable = false)
    private String senha;

    // E-mail único usado para identificação e login.
    @Column(nullable = false, unique = true)
    private String email;

    // Data de nascimento.
    @Convert(converter = LocalDateStringConverter.class)
    @Column
    private LocalDate datanasc;

    // Gênero do cliente.
    @Enumerated(EnumType.STRING)
    @Column
    private Genero gen;

    // CPF criptografado no banco via CpfConverter (LGPD).
    @Column
    private String cpf;

    // URL da foto de perfil.
    @Column
    private String fotoPerfil;

    // ID do endereço padrão de entrega.
    @Column
    private Long enderecoSelecionadoId;

    // Saldo atual de tokens do cliente.
    @Column(nullable = false)
    @Builder.Default
    private Double saldoTokens = 0.0;

    // Contador de tentativas de login falhas.
    @Column(nullable = false)
    @Builder.Default
    private int tentativas = 0;

    // True após 5 tentativas erradas consecutivas.
    @Column(nullable = false)
    @Builder.Default
    private boolean bloqueada = false;

    // False quando conta removida (soft delete).
    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    /**
     * Verificação de e-mail — false até o usuário clicar no link de confirmação.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerificado = false;

    // Estado da conta: ATIVO, SUSPENSO ou REMOVIDO.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'ATIVO'")
    @Builder.Default
    private StatusConta statusConta = StatusConta.ATIVO;

    // Data de cadastro, preenchida automaticamente.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime dataCriacao;

    // Data de remoção para soft delete.
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Data de término da suspensão.
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(name = "suspensao_ate", columnDefinition = "TEXT")
    private LocalDateTime suspensaoAte;

    // Motivo registrado pelo admin.
    @Column(name = "motivo_suspensao", length = 500)
    private String motivoSuspensao;

    // Data da última ação administrativa.
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(name = "data_acao", columnDefinition = "TEXT")
    private LocalDateTime dataAcao;

    // E-mail do admin responsável pela ação.
    @Column(name = "admin_acao", length = 255)
    private String adminAcao;

    // Data em que o cliente foi notificado por e-mail.
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(name = "email_notificado_em", columnDefinition = "TEXT")
    private LocalDateTime emailNotificadoEm;

    // Cartões vinculados ao cliente.
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "cliente_cartao", joinColumns = @JoinColumn(name = "cliente_id"), inverseJoinColumns = @JoinColumn(name = "cartao_id"))
    @Builder.Default
    private Set<Cartao> cartoes = new HashSet<>();

    // Endereços cadastrados pelo cliente.
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "cliente_endereco", joinColumns = @JoinColumn(name = "cliente_id"), inverseJoinColumns = @JoinColumn(name = "endereco_id"))
    @Builder.Default
    private Set<Endereco> enderecos = new HashSet<>();

    // Avaliações de livros feitas pelo cliente.
    @OneToMany(mappedBy = "avaliador")
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;

    // --- Métodos de Negócio (Encapsulamento) ---

    /** Incrementa o contador de tentativas e bloqueia a conta após 5 falhas. */
    public void registrarFalhaLogin() {
        this.tentativas++;
        if (this.tentativas >= 5) {
            this.bloqueada = true;
        }
    }

    /** Zera o contador de tentativas e desbloqueia a conta após login bem-sucedido. */
    public void resetarTentativas() {
        this.tentativas = 0;
        this.bloqueada = false;
    }
}
