package umc.exs.service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.notificacao.NotificacaoService;

/**
 * Scheduler de cupons:
 * - Meia-noite: marca como usado cupons expirados e não resgatados.
 * - 09h: envia aviso (e-mail + dashboard) de cupons a vencer em 7 dias.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CupomSchedulerService {

    private final CupomRepository cupomRepository;
    private final EmailService emailService;
    private final NotificacaoService notificacaoService;

    // ── Feature 7: Limpeza diária à meia-noite ─────────────────────────

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void marcarCuponsExpirados() {
        List<Cupom> expirados = cupomRepository.findByUsadoFalseAndExpiracaoBefore(LocalDateTime.now());

        if (expirados.isEmpty()) {
            return;
        }

        log.info("CupomScheduler: marcando {} cupom(ns) expirado(s) como usado.", expirados.size());
        for (Cupom cupom : expirados) {
            cupom.setUsado(true);
        }
        cupomRepository.saveAll(expirados);
    }

    // ── Feature 8: Aviso diário às 09h — cupons a vencer em 7 dias ─────

    @Transactional
    @Scheduled(cron = "0 0 9 * * *")
    public void avisarCuponsAVencer() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime em7Dias = agora.plusDays(7);

        // Cupons que vencem entre agora e 7 dias e ainda não foram usados
        List<Cupom> aVencer = cupomRepository.findByUsadoFalseAndExpiracaoBetween(agora, em7Dias);

        if (aVencer.isEmpty()) {
            return;
        }

        log.info("CupomScheduler: enviando aviso de vencimento para {} cupom(ns).", aVencer.size());

        for (Cupom cupom : aVencer) {
            // Apenas cupons nominais (vinculados a um cliente) recebem notificação
            Cliente cliente = cupom.getCliente();
            if (cliente == null) continue;

            try {
                emailService.enviarHtml(
                        cliente.getEmail(),
                        "Seu cupom vai expirar em breve! — Bibliotroca",
                        EmailHtmlBuilder.cupomExpirando(
                                cliente.getNome(), cupom.getCodigo(),
                                cupom.getValorTokens(),
                                cupom.getExpiracao().toLocalDate().toString()));

                notificacaoService.criarNotificacaoDashboard(
                        cliente,
                        "Seu cupom " + cupom.getCodigo() + " vence em " +
                                cupom.getExpiracao().toLocalDate() + ". Resgate antes que expire!",
                        "/carteira");

            } catch (Exception e) {
                log.error("Falha ao notificar cliente {} sobre vencimento do cupom {}: {}",
                        cliente.getEmail(), cupom.getCodigo(), e.getMessage());
            }
        }
    }
}
