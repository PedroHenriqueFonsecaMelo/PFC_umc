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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import umc.exs.model.entidades.usuario.Cliente;

/**
 * Armazena o token de recuperação de senha enviado por e-mail ao cliente;
 * o token expira após 24 horas e só pode ser usado uma vez.
 */
@Entity
@Table(name = "recuperacao_senha")

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Token único enviado por e-mail para redefinição de senha.
    @Column(nullable = false, unique = true)
    private String token;

    // Cliente que solicitou a recuperação.
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Data e hora de expiração do token.
    @Column(name = "expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    // E-mail do cliente para referência rápida.
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

    /** Verifica se o token já passou da data de expiração. */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.dataExpiracao);
    }

}