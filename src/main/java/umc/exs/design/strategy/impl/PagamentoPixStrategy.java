package umc.exs.design.strategy.impl;

import org.springframework.stereotype.Component;

import umc.exs.design.strategy.PagamentoStrategy;
import umc.exs.model.dtos.user.CompraTokensRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PagamentoPixStrategy implements PagamentoStrategy {
    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        log.info("Gerando QR Code PIX para valor: R$ {}", valor);
        // Simulação de geração de chave PIX
        return true; 
    }

    @Override
    public String getTipoPagamento() {
        return "PIX";
    }
}