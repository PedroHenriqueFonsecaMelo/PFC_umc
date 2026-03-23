package umc.exs.model.entidades.foundation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.usuario.Cliente;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @JsonIgnore
    private Cliente cliente;

    @Column(unique = true, nullable = false)
    private String codigoProtocolo;

    private LocalDateTime dataCriacao;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private LoteStatus status = LoteStatus.PENDENTE;

    public enum LoteStatus {
        PENDENTE, PARCIAL_APROVADO, TOTAL_APROVADO, REJEITADO
    }
}
