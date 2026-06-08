package umc.exs.model.entidades.logic;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import umc.exs.config.LocalDateTimeConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reporte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String emailContato;

    @Column(nullable = false)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Convert(converter = LocalDateTimeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private LocalDateTime dataCriacao;

    @Builder.Default
    private boolean lido = false;

    /** Status da conta no momento do reporte (pode ser null se e-mail não cadastrado) */
    private String statusConta;

    @Convert(converter = LocalDateTimeConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDateTime dataCadastro;

    private String nomeUsuario;
}
