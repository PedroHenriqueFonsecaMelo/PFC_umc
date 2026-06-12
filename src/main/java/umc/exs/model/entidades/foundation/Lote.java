package umc.exs.model.entidades.foundation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import umc.exs.model.entidades.usuario.Cliente;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Representa um lote de livros submetido pelo vendedor para venda em conjunto,
 * com código de protocolo único e status de aprovação pelo admin.
 */
@Entity

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lote_livros")
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vendedor que submeteu o lote.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @JsonIgnore
    private Cliente cliente;

    // Código único gerado para rastreamento do lote.
    @Column(unique = true, nullable = false)
    private String codigoProtocolo;

    // Data e hora de criação do lote.
    private LocalDateTime dataCriacao;

    // Estado atual do lote (PENDENTE, PARCIAL_APROVADO, etc.).
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private LoteStatus status = LoteStatus.PENDENTE;

    /** Define os possíveis estados de um lote no fluxo de aprovação. */
    public enum LoteStatus {
        PENDENTE, PARCIAL_APROVADO, TOTAL_APROVADO, REJEITADO
    }
}
