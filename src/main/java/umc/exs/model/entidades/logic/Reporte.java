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

/**
 * Representa um reporte ou denúncia enviado por um usuário via formulário
 * público, com dados de contato, motivo e informações da conta caso o
 * e-mail esteja cadastrado.
 */
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

    // E-mail informado pelo usuário para contato.
    @Column(nullable = false)
    private String emailContato;

    // Categoria do reporte (ex: bug, abuso, sugestão).
    @Column(nullable = false)
    private String motivo;

    // Descrição detalhada do reporte.
    @Column(columnDefinition = "TEXT")
    private String detalhes;

    // Data e hora em que o reporte foi enviado.
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private LocalDateTime dataCriacao;

    // False enquanto não analisado pelo admin.
    @Builder.Default
    private boolean lido = false;

    /** Status da conta no momento do reporte (pode ser null se e-mail não cadastrado) */
    private String statusConta;

    // Data de cadastro do cliente se e-mail encontrado.
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDateTime dataCadastro;

    // Nome do cliente se e-mail encontrado na base.
    private String nomeUsuario;
}
