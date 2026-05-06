package umc.exs.service.core.interactions;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.foundation.VisitaSite;
import umc.exs.repository.logic.VisitaSiteRepository;

@Service
@RequiredArgsConstructor
public class VisitaSiteService {

    private final VisitaSiteRepository visitaSiteRepository;

    /**
     * Incrementa o contador de visitas do dia atual.
     * Cria o registro do dia se ainda não existir.
     */
    @Transactional
    public void registrarVisita() {
        LocalDate hoje = LocalDate.now();
        VisitaSite visita = visitaSiteRepository.findByData(hoje)
                .orElseGet(() -> VisitaSite.builder().data(hoje).total(0L).build());
        visita.setTotal(visita.getTotal() + 1);
        visitaSiteRepository.save(visita);
    }
}
