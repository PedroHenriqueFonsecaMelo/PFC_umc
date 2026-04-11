package umc.exs.design.strategy;

import umc.exs.DTOs.compra.CompraTokensRequestDTO;

public interface PagamentoStrategy {

    boolean processar(double valor, CompraTokensRequestDTO dados);

    String getTipoPagamento();
}