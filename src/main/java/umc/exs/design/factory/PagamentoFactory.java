package umc.exs.design.factory;

import org.springframework.stereotype.Service;
import umc.exs.design.strategy.PagamentoStrategy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * Implementa o padrão Factory para instanciar a estratégia de pagamento correta (PIX, CARTAO) com base no método informado.
 * Centraliza a seleção da estratégia, desacoplando o código cliente das implementações concretas.
 */
@Service
public class PagamentoFactory {

    private final Map<String, PagamentoStrategy> estrategias;

    /**
     * Recebe todas as implementações de PagamentoStrategy injetadas pelo Spring e as mapeia pelo tipo de pagamento.
     * O mapa usa o valor retornado por getTipoPagamento() como chave (ex: "PIX", "CARTAO").
     */
    // Construtor: Cria um mapa onde a chave é o nome (CARTAO, PIX) e o valor é a
    // classe
    public PagamentoFactory(List<PagamentoStrategy> listaEstrategias) {
        // construir mapa manualmente para evitar streams
        Map<String, PagamentoStrategy> mapa = new HashMap<>();
        for (PagamentoStrategy estrategia : listaEstrategias) {
            if (estrategia != null && estrategia.getTipoPagamento() != null) {
                mapa.put(estrategia.getTipoPagamento(), estrategia);
            }
        }
        estrategias = mapa;
    }

    /**
     * Retorna a estratégia de pagamento correspondente ao método informado (ex: "pix", "cartao").
     * Lança IllegalArgumentException se o método não for suportado por nenhuma estratégia registrada.
     */
    public PagamentoStrategy buscarEstrategia(String metodo) {
        return Optional.ofNullable(estrategias.get(metodo.toUpperCase()))
                .orElseThrow(() -> new IllegalArgumentException("Método de pagamento '" + metodo + "' não suportado."));
    }
}