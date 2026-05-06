package umc.exs.service.core.interactions;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitaSiteService {

    private final LogAuditoriaService logAuditoria;

    /**
     * Registra uma visita à homepage no log de auditoria.
     * Usado pelo VisitaInterceptor.
     */
    public void registrarVisita() {
        // IP do cliente via header
        String ip = "127.0.0.1"; // Default para testes/local

        logAuditoria.registrarLog(
                "SITE_VISITA",
                0L, // Sem user ID para visitantes anônimos
                ip,
                "Visita à homepage");

        log.debug("Visita registrada do IP: {}", ip);
    }
}
