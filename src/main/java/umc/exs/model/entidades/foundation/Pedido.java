package umc.exs.model.entidades.foundation;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.StatusEnvio;

/**
 * Registra cada livro comprado pelo cliente.
 * Preserva título/autor/preço mesmo após o livro ser deletado da vitrine.
 * Controla o ciclo de envio via StatusEnvio.
 */
@Entity
@Table(name = "pedido")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quem comprou
    @ManyToOne(optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    private Cliente comprador;

    // Dados do livro preservados no momento da compra
    @Column(nullable = false)
    private Long livroId;

    @Column(nullable = false)
    private String tituloLivro;

    @Column(nullable = false)
    private String autorLivro;

    private String isbnLivro;

    @Column(columnDefinition = "TEXT")
    private String fotosUrls;

    @Column(nullable = false)
    private Double precoLivro;

    // Status de envio
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusEnvio statusEnvio = StatusEnvio.AGUARDANDO_ENVIO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCompra;

    private LocalDateTime dataAtualizacaoStatus;

    // Código de rastreio (preenchido quando EM_TRANSITO)
    private String codigoRastreio;

    // LGPD Art. 16 — retenção obrigatória por 5 anos
    @Column(name = "data_retencao_expira")
    private LocalDate dataRetencaoExpira;
}
