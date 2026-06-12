package umc.exs.model.entidades.logic;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import umc.exs.config.LocalDateTimeConverter;

/**
 * Representa a resposta do admin a um reporte de usuário, enviada por e-mail
 * e persistida no banco para histórico de atendimento.
 */
@Entity
@Table(name = "reporte_resposta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteResposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reporte ao qual esta resposta está vinculada.
    @ManyToOne
    @JoinColumn(name = "reporte_id", nullable = false)
    private Reporte reporte;

    // Texto da resposta enviada ao usuário.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    // Data e hora em que a resposta foi enviada.
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private LocalDateTime dataEnvio;
}
