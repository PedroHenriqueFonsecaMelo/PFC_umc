package umc.exs.service.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.repository.livro.LivroRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InflacaoAgendadaScheduler {

    private final LivroRepository livroRepository;

    @Scheduled(cron = "0 0 0 1 * ?")
    public void aplicarIpcaMensalGeral() {

        Double taxaIpcaDoMes = 0.38;

        log.info("Iniciando atualização mensal de preços. IPCA={}%", taxaIpcaDoMes);

        try {

            int atualizados =
                    livroRepository.aplicarInflacaoEmTodosAprovados(taxaIpcaDoMes);

            log.info(
                "Atualização concluída. {} livros tiveram seus preços reajustados.",
                atualizados
            );

        } catch (Exception e) {

            log.error(
                "Erro ao aplicar reajuste mensal de preços.",
                e
            );
        }
    }
}