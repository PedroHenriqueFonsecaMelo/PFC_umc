package umc.exs.model.entidades.foundation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Representa a reserva temporária de um livro durante o processo de checkout.
 * Garante que o mesmo exemplar não seja comprado por dois clientes simultaneamente.
 * Possui mecanismo anti-abuso que bloqueia o cliente após 3 tentativas abandonadas.
 */
@Entity
@Table(name = "reserva_checkout")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Livro que está sendo reservado para o checkout
    @Column(nullable = false)
    private Long livroId;

    // Cliente que iniciou o processo de checkout
    @Column(nullable = false)
    private Long clienteId;

    // Momento em que a reserva foi criada
    @Column(nullable = false)
    private LocalDateTime reservadoEm;

    // Prazo de expiração da reserva; após este momento, o livro fica disponível novamente
    @Column(nullable = false)
    private LocalDateTime expiraEm;

    // Contador de tentativas abandonadas (anti-abuso)
    @Column(nullable = false)
    @Builder.Default
    private int tentativas = 0;

    // Bloqueio anti-abuso: se >= 3 tentativas, bloqueia por 5 min
    private LocalDateTime bloqueadoAte;
}
