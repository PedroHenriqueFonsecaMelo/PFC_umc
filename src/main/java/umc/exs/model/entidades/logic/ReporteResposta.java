package umc.exs.model.entidades.logic;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import umc.exs.config.LocalDateTimeConverter;

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

    @ManyToOne
    @JoinColumn(name = "reporte_id", nullable = false)
    private Reporte reporte;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    @Convert(converter = LocalDateTimeConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private LocalDateTime dataEnvio;
}
