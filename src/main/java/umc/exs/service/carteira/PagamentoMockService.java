package umc.exs.service.carteira;

import org.springframework.stereotype.Service;

@Service
public class PagamentoMockService {

    public boolean realizarPagamento(String numeroCartao, Double valor) {
        // Simulação: Cartão que termina em "0000" é recusado
        if (numeroCartao.endsWith("0000")) {
            return false;
        }
        // Simulação de sucesso
        return true;
    }
}