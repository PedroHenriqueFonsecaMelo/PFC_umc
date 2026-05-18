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
import io.github.manoelcampos.dtogen.DTO;
import umc.exs.model.entidades.usuario.Cliente;

@Entity
@Table(name = "cupom")
@DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    /** null = cupom público (qualquer cliente pode usar). */
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Percentual de desconto aplicado ao produto (ex.: 15.0 = 15%). */
    @Column(nullable = false)
    private Double percentualDesconto;

    /** Mantido para compatibilidade com banco existente (coluna NOT NULL legada). Sempre 0.0. */
    @Column(name = "valor_tokens", nullable = false)
    @Builder.Default
    private Double valorTokens = 0.0;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    /** Limite máximo de utilizações — null = ilimitado. */
    private Integer quantidadeMaxima;

    /** Quantas vezes o cupom já foi utilizado. */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantidadeUsada = 0;

    /**
     * Indica se o cupom está inativo (esgotado, invalidado ou expirado pelo scheduler).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    /** "PONTUACAO" ou "PROMOCIONAL" */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;
}
