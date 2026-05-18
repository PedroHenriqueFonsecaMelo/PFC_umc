package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import io.github.manoelcampos.dtogen.DTO;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.MotivoCategoria;
import umc.exs.model.enums.StatusSolicitacao;

@Entity
@Table(name = "solicitacao_cancelamento")
@DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoCancelamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoCategoria motivoCategoria;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivoDescricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String comentarioAdmin;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataSolicitacao;

    private LocalDateTime dataResposta;
}
