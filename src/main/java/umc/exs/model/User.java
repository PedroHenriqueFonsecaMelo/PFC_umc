package umc.exs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nome;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(nullable = false)
    private boolean bloqueada = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

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