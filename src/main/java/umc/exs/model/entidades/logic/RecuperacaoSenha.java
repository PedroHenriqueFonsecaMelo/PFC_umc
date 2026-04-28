package umc.exs.model.entidades.logic;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.usuario.Cliente;

@Entity
@Table(name = "recuperacao_senha")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(nullable = false)
    private String email;

    public RecuperacaoSenha(String token, Cliente cliente, LocalDateTime dataExpiracao) {
        this.token = token;
        this.cliente = cliente;
        this.dataExpiracao = dataExpiracao;
    }

    public RecuperacaoSenha(String email, String token, LocalDateTime expiracao) {
        this.email = email;
        this.token = token;
        this.dataExpiracao = expiracao;
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.dataExpiracao);
    }

}