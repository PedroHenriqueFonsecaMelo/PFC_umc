package umc.exs.model.entidades.foundation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Registra o histórico de e-mails disparados pelo admin, incluindo assunto,
 * filtro de destinatários, quantidade enviada e se foi imediato ou agendado.
 */
@Entity
@Table(name = "email_enviado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEnviado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String assunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corpo;

    @Column(nullable = false)
    private String filtro;

    @Column(nullable = false)
    private int quantidadeDestinatarios;

    @Column(nullable = false)
    private LocalDateTime dataRegistro;

    /** Null = envio imediato; preenchido = agendado para esta data/hora */
    @Column
    private LocalDateTime agendadoPara;

    /** "IMEDIATO" ou "AGENDADO" */
    @Column(nullable = false)
    private String tipo;
}
