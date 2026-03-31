package umc.exs.model.entidades.foundation;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Armazena o total de visitas por dia.
 * Incrementado pelo VisitaInterceptor a cada requisição de página web.
 */
@Entity
@Table(name = "visita_site")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitaSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate data;

    @Column(nullable = false)
    @Builder.Default
    private Long total = 0L;
}
