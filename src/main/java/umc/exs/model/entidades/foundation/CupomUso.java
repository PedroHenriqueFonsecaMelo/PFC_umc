package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.github.manoelcampos.dtogen.DTO;
import umc.exs.model.entidades.usuario.Cliente;

/**
 * Registra qual cliente usou qual cupom e em qual livro.
 * A unique constraint (cupom_id, cliente_id) garante que cada cliente
 * só use o mesmo cupom uma única vez.
 */
@Entity
@Table(name = "cupom_uso",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cupom_id", "cliente_id"}))
@DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CupomUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cupom_id", nullable = false)
    private Cupom cupom;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** ID do livro ao qual o desconto foi aplicado. */
    private Long livroId;

    @Column(nullable = false)
    private LocalDateTime dataUso;
}
