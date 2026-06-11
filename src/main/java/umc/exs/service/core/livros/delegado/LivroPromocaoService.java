package umc.exs.service.core.livros.delegado;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.livro.Livro;

/**
 * Serviço responsável por aplicar ou remover promoções de preço em livros.
 * Centraliza a lógica de desconto para garantir consistência em todo o sistema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LivroPromocaoService {

    /**
     * Aplica ou remove uma promoção no livro conforme o parâmetro promoAtiva.
     * Quando ativa, calcula o preço promocional e define a data de expiração.
     */
    public void aplicarPromocao(Livro livro, boolean promoAtiva, Double preco, Double percentualDesconto,
            java.time.LocalDateTime promocaoExpira) {
        if (promoAtiva) {
            // Preserva o preço original e calcula o preço com desconto percentual
            livro.setPrecoOriginal(preco);
            double precoPromo = preco * (1.0 - percentualDesconto / 100.0);
            livro.setPrecoAprovado(precoPromo);
            livro.setPromocaoExpira(promocaoExpira);
        } else {
            // Remove a promoção e restaura o preço aprovado sem desconto
            livro.setPrecoOriginal(null);
            livro.setPrecoAprovado(preco);
            livro.setPromocaoExpira(null);
        }
    }
}
