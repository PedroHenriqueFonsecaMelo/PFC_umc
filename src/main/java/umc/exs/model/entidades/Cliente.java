package umc.exs.model.entidades;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String datanasc;
    
    @Column(nullable = false)
    private String gen;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(nullable = false)
    private boolean bloqueada = false;

    @ManyToMany
    @JoinTable(
        name = "cliente_cartao",
        joinColumns = @JoinColumn(name = "cliente_id"),
        inverseJoinColumns = @JoinColumn(name = "cartao_id")
    )
    private Set<Cartao> cartoes = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "cliente_endereco",
        joinColumns = @JoinColumn(name = "cliente_id"),
        inverseJoinColumns = @JoinColumn(name = "endereco_id")
    )
    private Set<Endereco> enderecos = new HashSet<>();

    // Getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDatanasc() { return datanasc; }
    public void setDatanasc(String datanasc) { this.datanasc = datanasc; }

    public String getGen() { return gen; }
    public void setGen(String gen) { this.gen = gen; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<Cartao> getCartoes() { return cartoes; }
    public void setCartoes(Set<Cartao> cartoes) { this.cartoes = cartoes; }

    public Set<Endereco> getEnderecos() { return enderecos; }
    public void setEnderecos(Set<Endereco> enderecos) { this.enderecos = enderecos; }

    public void setFalhas() {
        if(this.tentativas >= 5){
            this.bloqueada = true;
        }
        else {
            this.tentativas++;
        }
    }

    public void logado () {
        this.tentativas = 0;
        this.bloqueada = false;
    }

    public boolean ContaBloqueada (){
        return bloqueada;
    }
}