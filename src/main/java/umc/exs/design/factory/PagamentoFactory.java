package umc.exs.design.factory;

import org.springframework.stereotype.Service;
import umc.exs.design.strategy.PagamentoStrategy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PagamentoFactory {

    private final Map<String, PagamentoStrategy> estrategias;

    // Construtor: Cria um mapa onde a chave é o nome (CARTAO, PIX) e o valor é a classe
    public PagamentoFactory(List<PagamentoStrategy> listaEstrategias) {
        estrategias = listaEstrategias.stream()
                .collect(Collectors.toMap(
                    PagamentoStrategy::getTipoPagamento, 
                    Function.identity()
                ));
    }

    public PagamentoStrategy buscarEstrategia(String metodo) {
        return Optional.ofNullable(estrategias.get(metodo.toUpperCase()))
                .orElseThrow(() -> new IllegalArgumentException("Método de pagamento '" + metodo + "' não suportado."));
    }
}