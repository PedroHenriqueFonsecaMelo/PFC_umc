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

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private Double valor;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "metodo_pagamento")
    private String metodoPagamento;

    @Column(name = "final_cartao")
    private String finalCartao;

    @Column(name = "pagamento_id")
    private String pagamentoId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDENTE";

    // LGPD Art. 16 — retenção obrigatória por 5 anos
    @Column(name = "data_retencao_expira")
    private LocalDate dataRetencaoExpira;

    @PrePersist
    public void definirDataRetencao() {
        if (this.dataHora != null && this.dataRetencaoExpira == null) {
            this.dataRetencaoExpira = this.dataHora.toLocalDate().plusYears(5);
        }
    }
}
