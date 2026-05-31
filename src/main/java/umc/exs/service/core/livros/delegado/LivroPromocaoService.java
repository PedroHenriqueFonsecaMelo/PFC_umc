package umc.exs.service.core.livros.delegado;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.livro.Livro;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroPromocaoService {

    public void aplicarPromocao(Livro livro, boolean promoAtiva, Double preco, Double percentualDesconto,
            java.time.LocalDateTime promocaoExpira) {
        // Regra centralizada (mantém comportamento atual)
        if (promoAtiva) {
            livro.setPrecoOriginal(preco);
            double precoPromo = preco * (1.0 - percentualDesconto / 100.0);
            livro.setPrecoAprovado(precoPromo);
            livro.setPromocaoExpira(promocaoExpira);
        } else {
            livro.setPrecoOriginal(null);
            livro.setPrecoAprovado(preco);
            livro.setPromocaoExpira(null);
        }
    }
}
