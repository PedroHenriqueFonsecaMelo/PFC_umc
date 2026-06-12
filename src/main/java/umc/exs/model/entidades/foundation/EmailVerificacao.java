package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import umc.exs.model.entidades.usuario.Cliente;

/**
 * Armazena o token de verificação de e-mail enviado ao cliente no cadastro;
 * o acesso só é liberado após o cliente clicar no link com o token válido.
 */
@Entity
@Table(name = "email_verificacao")

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cliente que deve verificar o e-mail.
    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Token único enviado por e-mail, expira em 24 horas.
    @Column(nullable = false, unique = true)
    private String token;

    // Data e hora de expiração do token.
    @Column(nullable = false)
    private LocalDateTime expiracao;

    // True quando o token já foi utilizado e não pode ser reusado.
    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    /** Verifica se o token já passou da data de expiração. */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(expiracao);
    }
}
