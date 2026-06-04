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
@Table(name = "notificacao_dashboard")

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoDashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private String mensagem;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    @Builder.Default
    private boolean lida = false;

    /** Link opcional para direcionar o usuário (ex: "/carteira"). */
    @Column
    private String link;
}
