package umc.exs.design.strategy;

import umc.exs.model.dtos.user.CompraTokensRequestDTO;

public interface PagamentoStrategy {
    
    boolean processar(double valor, CompraTokensRequestDTO dados);
    
    String getTipoPagamento();
}