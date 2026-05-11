package umc.exs.service.scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
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
    private final LogAuditoriaService logAuditoria;
    private final NotificacaoService notificacaoService;

    /**
     * Meia-noite: Processa a perda de XP por inatividade.
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void processarDecayXp() {
        // Quem não ganha XP há mais de 30 dias
        LocalDateTime limite = LocalDateTime.now().minusDays(30);
        List<PontuacaoUsuario> inativos = pontuacaoRepository.findAllByUltimaAtualizacaoBefore(limite);

        for (PontuacaoUsuario p : inativos) {
            long diasInativo = ChronoUnit.DAYS.between(p.getUltimaAtualizacao(), LocalDateTime.now());
            long diasDePerdaEfetiva = diasInativo - 30; // Dias após o 30º dia

            if (diasDePerdaEfetiva >= 15) {
                zerarTudo(p);
            } else {
                // Decay Linear: perde 1/15 (aprox 6.6%) do XP a cada dia de inatividade extra
                double fatorPermanencia = 1.0 - (diasDePerdaEfetiva / 15.0);
                aplicarReducaoProporcional(p, fatorPermanencia);
            }
        }
        pontuacaoRepository.saveAll(inativos);
    }

    private void aplicarReducaoProporcional(PontuacaoUsuario p, double fator) {
        p.setXpTotal((int) (p.getXpTotal() * fator));
        p.setXpLivrosAprovados((int) (p.getXpLivrosAprovados() * fator));
        p.setXpCompras((int) (p.getXpCompras() * fator));
        p.setXpAvaliacoes((int) (p.getXpAvaliacoes() * fator));
        log.info("Decay aplicado para {}: XP atualizado para {}", p.getCliente().getEmail(), p.getXpTotal());
    }

    private void zerarTudo(PontuacaoUsuario p) {
        p.setXpTotal(0);
        p.setXpLivrosAprovados(0);
        p.setXpCompras(0);
        p.setXpAvaliacoes(0);
        
        logAuditoria.registrarLog("XP_ZERADO", p.getCliente().getId(), p.getCliente().getEmail(), "XP zerado por 45 dias de inatividade.");
        notificacaoService.criarNotificacaoDashboard(p.getCliente(), "Seu XP foi zerado por inatividade prolongada.", "/gamificacao");
    }
}