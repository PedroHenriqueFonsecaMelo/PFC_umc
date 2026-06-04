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

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(expiracao);
    }
}
