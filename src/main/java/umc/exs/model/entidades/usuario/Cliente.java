package umc.exs.model.entidades.usuario;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.foundation.enums.Genero;

@Entity
@Table(name = "users")
@Data // Inclui @Getter, @Setter, @ToString, @EqualsAndHashCode e
      // @RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id") // Garante que a comparação seja feita apenas pelo ID
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String datanasc; // Considere usar LocalDate

    @Column(nullable = false)
    private Genero gen;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(nullable = false)
    private boolean bloqueada = false;

    // --- CAMPO ADICIONADO PARA DATA DE CRIAÇÃO ---
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    // Relacionamento ManyToMany - Removido CascadeType.ALL para evitar deleção
    // acidental
    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            jakarta.persistence.CascadeType.PERSIST, // Para novos cartões
            jakarta.persistence.CascadeType.MERGE // Para gerenciar remoção/adição na tabela de junção
    })
    @JoinTable(name = "cliente_cartao", joinColumns = @JoinColumn(name = "cliente_id"), inverseJoinColumns = @JoinColumn(name = "cartao_id"))
    private Set<Cartao> cartoes = new HashSet<>();

    // Relacionamento ManyToMany - Removido CascadeType.ALL para evitar deleção
    // acidental
    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            jakarta.persistence.CascadeType.PERSIST, // Para novos cartões
            jakarta.persistence.CascadeType.MERGE // Para gerenciar remoção/adição na tabela de junção
    })
    @JoinTable(name = "cliente_endereco", joinColumns = @JoinColumn(name = "cliente_id"), inverseJoinColumns = @JoinColumn(name = "endereco_id"))
    private Set<Endereco> enderecos = new HashSet<>();

    // --- Métodos de Negócio ---

    public void setFalhas() {
        if (this.tentativas >= 5) {
            this.bloqueada = true;
        } else {
            this.tentativas++;
        }
    }

    public void logado() {
        this.tentativas = 0;
        this.bloqueada = false;
    }

    public boolean isContaBloqueada() {
        return bloqueada;
    }
}