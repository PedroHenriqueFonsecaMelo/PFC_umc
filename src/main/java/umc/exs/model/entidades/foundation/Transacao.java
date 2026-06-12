package umc.exs.model.entidades.foundation;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import umc.exs.model.entidades.usuario.Cliente;

/**
 * Representa uma transação financeira de compra de tokens via PIX, com status
 * de pagamento e retenção obrigatória de 5 anos conforme LGPD Art. 16.
 */
@Entity
@Table(name = "transacao")

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cliente que realizou a transação.
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Valor em reais da transação.
    @Column(nullable = false)
    private Double valor;

    // Data e hora da transação.
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // Método usado (ex: PIX).
    @Column(name = "metodo_pagamento")
    private String metodoPagamento;

    // Últimos dígitos do cartão ou descrição da transação.
    @Column(name = "final_cartao")
    private String finalCartao;

    // ID do pagamento no Mercado Pago.
    @Column(name = "pagamento_id")
    private String pagamentoId;

    // Estado do pagamento: PENDENTE ou APROVADO.
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDENTE";

    // LGPD Art. 16 — retenção obrigatória por 5 anos
    @Column(name = "data_retencao_expira")
    private LocalDate dataRetencaoExpira;

    /**
     * Define automaticamente a data de expiração de retenção como 5 anos após
     * a transação, em conformidade com a LGPD.
     */
    @PrePersist
    public void definirDataRetencao() {
        if (this.dataHora != null && this.dataRetencaoExpira == null) {
            this.dataRetencaoExpira = this.dataHora.toLocalDate().plusYears(5);
        }
    }

    /**
     * Factory method para criação padronizada de transações com todos os campos
     * obrigatórios.
     */
    public static Transacao criarTransacao(
            Cliente cliente,
            Double valor,
            String metodo,
            String status,
            String descricao) {

        return Transacao.builder()
                .cliente(cliente)
                .valor(valor)
                .metodoPagamento(metodo)
                .status(status)
                .finalCartao(descricao)
                .dataHora(LocalDateTime.now())
                .build();
    }
}
