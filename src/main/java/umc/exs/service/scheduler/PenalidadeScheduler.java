package umc.exs.service.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import umc.exs.service.gamificacao.PenalidadeService;

@Component
public class PenalidadeScheduler {

    @Autowired
    private PenalidadeService penalidadeService;

    @Scheduled(cron = "0 0 0 * * *")
    public void executarPenalidade() {
        penalidadeService.aplicarPenalidadeTodos();
    }
}