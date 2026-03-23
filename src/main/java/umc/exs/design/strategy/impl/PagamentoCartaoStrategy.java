package umc.exs.design.strategy.impl;

import org.springframework.stereotype.Component;

import umc.exs.DTOs.compra.CompraTokensRequestDTO;
import umc.exs.design.strategy.PagamentoStrategy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PagamentoCartaoStrategy implements PagamentoStrategy {
    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        log.info("Processando pagamento de R$ {} via CARTÃO: {}", valor, dados.getNumeroCartao());
        // Simulação de lógica de cartão
        return dados.getNumeroCartao() != null && !dados.getNumeroCartao().isBlank();
    }

    @Override
    public String getTipoPagamento() {
        return "CARTAO";
    }
}