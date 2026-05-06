package umc.exs.model.entidades.foundation;

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

@Entity
@Table(name = "sessao_ativa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessaoAtiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** SHA-256 hash do token JWT para evitar armazenar o token completo. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime dataLogin;

    @Column
    private LocalDateTime dataLogout;

    @Column(length = 50)
    private String ip;

    @Column(length = 255)
    private String userAgent;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativa = true;
}
