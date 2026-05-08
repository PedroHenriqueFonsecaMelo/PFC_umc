package umc.exs.service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

/**
 * Scheduler de pontos XP:
 * - Meia-noite: zera XP de pontuações expiradas e registra log de auditoria.
 * - 09h:        avisa (e-mail + dashboard) usuários com XP a vencer em 30 dias.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PontosSchedulerService {

    private final PontuacaoUsuarioRepository pontuacaoRepository;
    private final EmailService emailService;
    private final NotificacaoService notificacaoService;
    private final LogAuditoriaService logAuditoria;

    // ── 11.3.1 Limpeza diária à meia-noite ────────────────────────────────

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void limparPontosExpirados() {
        List<PontuacaoUsuario> expirados = pontuacaoRepository.findExpirados(LocalDateTime.now());

        if (expirados.isEmpty()) {
            return;
        }

        log.info("PontosScheduler: zerando XP de {} pontuação(ões) expirada(s).", expirados.size());

        for (PontuacaoUsuario pontuacao : expirados) {
            Cliente cliente = pontuacao.getCliente();
            int xpAnterior = pontuacao.getXpTotal();
            String dataExpiracaoStr = pontuacao.getDataExpiracao().toLocalDate().toString();

            pontuacao.setXpTotal(0);
            pontuacao.setXpLivrosAprovados(0);
            pontuacao.setXpCompras(0);
            pontuacao.setXpAvaliacoes(0);
            pontuacao.setUltimaAtualizacao(LocalDateTime.now());
            pontuacao.setDataExpiracao(null);

            try {
                logAuditoria.registrarLog(
                        "XP_EXPIRADO",
                        cliente.getId(),
                        cliente.getEmail(),
                        xpAnterior + " XP expirados e removidos (prazo encerrado em " +
                                dataExpiracaoStr + ")");

                notificacaoService.criarNotificacaoDashboard(
                        cliente,
                        "Seus " + xpAnterior + " pontos XP expiraram em " + dataExpiracaoStr +
                                ". Continue participando para acumular novos pontos!",
                        "/gamificacao");

            } catch (Exception e) {
                log.error("Falha ao notificar cliente {} sobre expiração de XP: {}",
                        cliente.getEmail(), e.getMessage());
            }
        }

        pontuacaoRepository.saveAll(expirados);
    }

    // ── 11.3.2 Aviso diário às 09h — XP a vencer em 30 dias ──────────────

    @Transactional
    @Scheduled(cron = "0 0 9 * * *")
    public void avisarPontosAVencer() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime em30Dias = agora.plusDays(30);

        List<PontuacaoUsuario> aVencer = pontuacaoRepository.findAVencer(agora, em30Dias);

        if (aVencer.isEmpty()) {
            return;
        }

        log.info("PontosScheduler: enviando aviso de vencimento para {} usuário(s).", aVencer.size());

        for (PontuacaoUsuario pontuacao : aVencer) {
            Cliente cliente = pontuacao.getCliente();

            try {
                String dataExpiracao = pontuacao.getDataExpiracao().toLocalDate().toString();

                emailService.enviarHtml(
                        cliente.getEmail(),
                        "Seus pontos XP vão expirar em breve! — Bibliotroca",
                        EmailHtmlBuilder.xpExpirando(
                                cliente.getNome(), pontuacao.getXpTotal(), dataExpiracao));

                notificacaoService.criarNotificacaoDashboard(
                        cliente,
                        "Seus " + pontuacao.getXpTotal() + " pontos XP expiram em " +
                                dataExpiracao + ". Realize uma ação para renová-los!",
                        "/gamificacao");

            } catch (Exception e) {
                log.error("Falha ao notificar cliente {} sobre vencimento de XP: {}",
                        cliente.getEmail(), e.getMessage());
            }
        }
    }
}
