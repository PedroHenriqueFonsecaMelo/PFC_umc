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

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.MotivoCategoria;
import umc.exs.model.enums.StatusSolicitacao;

/**
 * Representa uma solicitação de cancelamento de pedido feita pelo cliente,
 * com motivo, status e comentário do admin após análise.
 */
@Entity
@Table(name = "solicitacao_cancelamento")

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoCancelamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Pedido que está sendo solicitado o cancelamento.
    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Cliente que abriu a solicitação.
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Categoria do motivo (enum MotivoCategoria).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoCategoria motivoCategoria;

    // Descrição detalhada do motivo pelo cliente.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivoDescricao;

    // Estado da solicitação: PENDENTE, APROVADO ou RECUSADO.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    // Resposta do admin ao analisar a solicitação.
    @Column(columnDefinition = "TEXT")
    private String comentarioAdmin;

    // Data e hora de abertura da solicitação.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataSolicitacao;

    // Data e hora da resposta do admin.
    private LocalDateTime dataResposta;
}
