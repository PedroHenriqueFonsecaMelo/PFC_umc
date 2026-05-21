package umc.exs.design.strategy;

import umc.exs.dto.request.compra.CompraTokensRequest;

public interface PagamentoStrategy {

    boolean processar(double valor, CompraTokensRequest dados);

    String getTipoPagamento();
}