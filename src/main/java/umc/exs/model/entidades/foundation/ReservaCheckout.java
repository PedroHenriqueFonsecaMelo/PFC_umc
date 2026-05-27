package umc.exs.model.entidades.foundation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private Long livroId;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private LocalDateTime reservadoEm;

    @Column(nullable = false)
    private LocalDateTime expiraEm;

    // Contador de tentativas abandonadas (anti-abuso)
    @Column(nullable = false)
    @Builder.Default
    private int tentativas = 0;

    // Bloqueio anti-abuso: se >= 3 tentativas, bloqueia por 5 min
    private LocalDateTime bloqueadoAte;
}
