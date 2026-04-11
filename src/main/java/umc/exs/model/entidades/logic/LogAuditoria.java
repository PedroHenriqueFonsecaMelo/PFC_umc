package umc.exs.model.entidades.logic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log_auditoria")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LogAuditoria {

    private static final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID do usuário alvo da ação
    @Column(name = "id_usuario", nullable = true)
    private Long idUsuario;

    // Email do usuário (para referência rápida no banco)
    @Column(name = "email_usuario", length = 100, nullable = true)
    private String emailUsuario;

    // Tipo de ação realizada (ex: LOGIN_SUCESSO)
    @Column(name = "acao", length = 50, nullable = false)
    private String acao;

    // Mensagem detalhada sobre a ação
    @Column(name = "detalhes", columnDefinition = "TEXT")
    private String detalhes;

    // Data e hora da ocorrência do log
    @Column(name = "data_hora", nullable = false)
    private String dataHora;

    public LogAuditoria(Long idUsuario, String emailUsuario, String acao, String detalhes, LocalDateTime dataHora) {
        this.idUsuario = idUsuario;
        this.emailUsuario = emailUsuario;
        this.acao = acao;
        this.detalhes = detalhes;
        this.dataHora = dataHora.format(f);
    }

}
