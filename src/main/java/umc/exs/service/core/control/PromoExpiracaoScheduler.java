package umc.exs.service.core.control;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.repository.livro.LivroRepository;

/**
 * Scheduler que expira promoções de livros automaticamente.
 * Roda a cada minuto; para cada livro com promocaoExpira < now():
 * - emPromocao  → false
 * - precoAprovado → precoOriginal (restaura preço original)
 * - precoOriginal → null
 * - promocaoExpira → null
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromoExpiracaoScheduler {

    private final LivroRepository livroRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expirarPromocoes() {
        List<Livro> expirados = livroRepository.findPromocoesExpiradas(LocalDateTime.now());
        if (expirados.isEmpty()) return;

        for (Livro livro : expirados) {
            Double precoOriginal = livro.getPrecoOriginal();
            livro.setEmPromocao(false);
            livro.setPrecoAprovado(precoOriginal != null ? precoOriginal : livro.getPrecoAprovado());
            livro.setPrecoOriginal(null);
            livro.setPromocaoExpira(null);
            livroRepository.save(livro);
            log.info("Promoção expirada: livro ID {} — \"{}\" | preço restaurado para T$ {}",
                    livro.getId(), livro.getTitulo(), livro.getPrecoAprovado());
        }

        log.info("Scheduler promoções: {} promoção(ões) expirada(s).", expirados.size());
    }
}
