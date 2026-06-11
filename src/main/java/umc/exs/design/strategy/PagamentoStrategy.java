package umc.exs.design.strategy;

import umc.exs.dto.request.compra.CompraTokensRequest;

/**
 * Define o contrato do padrão Strategy para processamento de pagamentos.
 * Permite adicionar novos métodos de pagamento (ex: boleto, cartão de débito) sem alterar o código existente.
 */
public interface PagamentoStrategy {

    /**
     * Executa o pagamento com o valor em reais e os dados da compra informados.
     * Retorna true se o processamento for bem-sucedido.
     */
    boolean processar(double valor, CompraTokensRequest dados);

    /**
     * Retorna o identificador do método de pagamento (ex: "PIX", "CARTAO").
     * Utilizado pelo PagamentoFactory para selecionar a estratégia correta.
     */
    String getTipoPagamento();
}
