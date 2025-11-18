package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import umc.exs.model.entidades.usuario.Cliente;

@Entity
@Table(name = "recuperacao_senha")
public class RecuperacaoSenha {

    private static final int EXPIRATION_TIME_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = Cliente.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "cliente_id")
    private Cliente cliente; // Assumimos que a entidade Cliente existe

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    public RecuperacaoSenha() {
    }

    public RecuperacaoSenha(String token, Cliente cliente) {
        this.token = token;
        this.cliente = cliente;
        this.dataExpiracao = LocalDateTime.now().plusMinutes(EXPIRATION_TIME_MINUTES);
    }

    // --- Métodos de Negócio ---

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(dataExpiracao);
    }

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
}
