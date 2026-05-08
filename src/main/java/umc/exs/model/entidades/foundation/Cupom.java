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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.usuario.Cliente;

@Entity
@Table(name = "cupom")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    /** null = cupom público (qualquer cliente pode usar). */
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false)
    private Double valorTokens;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    /** "PONTUACAO" ou "PROMOCIONAL" */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;
}
