package umc.exs.service.core.control;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import umc.exs.DTOs.admin.EmailDestinatarioDTO;
import umc.exs.DTOs.admin.EmailDisparoDTO;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoEmailService {

    private final ClienteRepository clienteRepository;
    private final PontuacaoUsuarioRepository pontuacaoRepository;
    private final EmailService emailService;
    private final TaskScheduler taskScheduler;

    /**
     * Formato produzido por inputs datetime-local do HTML: "yyyy-MM-dd'T'HH:mm"
     * LocalDateTime.parse() padrão exige segundos — por isso precisamos do formatter.
     */
    private static final DateTimeFormatter DATETIME_LOCAL_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private static final DateTimeFormatter EXIBICAO_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    // ── Filtrar destinatários ────────────────────────────────────────────────

    public List<EmailDestinatarioDTO> filtrarDestinatarios(String filtro, Integer limite) {
        int lim = (limite == null || limite <= 0) ? Integer.MAX_VALUE : limite;

        List<Cliente> clientes = clienteRepository.findAll();

        List<PontuacaoUsuario> pontuacoes = pontuacaoRepository.findAllWithCliente();
        Map<Long, Integer> xpMap = pontuacoes.stream()
                .collect(Collectors.toMap(
                        p -> p.getCliente().getId(),
                        PontuacaoUsuario::getXpTotal
                ));

        Comparator<Cliente> comparator = switch (filtro == null ? "todos" : filtro) {
            case "ranking_maior" -> Comparator.comparingInt(
                    (Cliente c) -> xpMap.getOrDefault(c.getId(), 0)).reversed();
            case "ranking_menor" -> Comparator.comparingInt(
                    (Cliente c) -> xpMap.getOrDefault(c.getId(), 0));
            case "tokens_maior" -> Comparator.comparingDouble(
                    (Cliente c) -> c.getSaldoTokens() == null ? 0.0 : c.getSaldoTokens()).reversed();
            case "tokens_menor" -> Comparator.comparingDouble(
                    (Cliente c) -> c.getSaldoTokens() == null ? 0.0 : c.getSaldoTokens());
            case "mais_antigos" -> Comparator.comparing(
                    (Cliente c) -> c.getDataCriacao() == null ? LocalDateTime.now() : c.getDataCriacao());
            case "mais_novos" -> Comparator.comparing(
                    (Cliente c) -> c.getDataCriacao() == null ? LocalDateTime.now() : c.getDataCriacao()).reversed();
            default -> Comparator.comparing(Cliente::getId);
        };

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return clientes.stream()
                .sorted(comparator)
                .limit(lim)
                .map(c -> new EmailDestinatarioDTO(
                        c.getId(),
                        c.getNome(),
                        c.getEmail(),
                        c.getSaldoTokens() == null ? 0.0 : c.getSaldoTokens(),
                        xpMap.getOrDefault(c.getId(), 0),
                        c.getDataCriacao() != null ? c.getDataCriacao().format(fmt) : "—"
                ))
                .collect(Collectors.toList());
    }

    // ── Disparar ou agendar ──────────────────────────────────────────────────

    public String dispararOuAgendar(EmailDisparoDTO dto) {
        log.info("dispararOuAgendar — filtro='{}', limite={}, assunto='{}'",
                dto.getFiltro(), dto.getLimite(), dto.getAssunto());

        List<EmailDestinatarioDTO> destinatarios =
                filtrarDestinatarios(dto.getFiltro(), dto.getLimite());

        if (destinatarios.isEmpty()) {
            log.warn("Nenhum destinatário encontrado para filtro='{}'", dto.getFiltro());
            return "Nenhum destinatário encontrado para o filtro selecionado.";
        }

        log.info("{} destinatário(s) selecionado(s).", destinatarios.size());

        // Runnable usa EmailHtmlBuilder + enviarHtml() igual aos outros emails do sistema
        Runnable tarefa = () -> {
            log.info(">>> Iniciando disparo: assunto='{}', {} destinatário(s)",
                    dto.getAssunto(), destinatarios.size());
            int enviados = 0, erros = 0;
            for (EmailDestinatarioDTO dest : destinatarios) {
                try {
                    emailService.enviarHtml(
                            dest.getEmail(),
                            dto.getAssunto(),
                            EmailHtmlBuilder.comunicadoAdmin(dest.getNome(), dto.getCorpo()));
                    enviados++;
                    log.debug("✓ Enviado para: {}", dest.getEmail());
                } catch (Exception e) {
                    erros++;
                    log.error("✗ Falha ao enviar para {} — {}: {}",
                            dest.getEmail(), e.getClass().getSimpleName(), e.getMessage());
                }
            }
            log.info("<<< Disparo concluído — enviados: {}, erros: {}", enviados, erros);
        };

        String agendamento = dto.getAgendamento();
        if (agendamento != null && !agendamento.isBlank()) {
            // ── AGENDADO ──────────────────────────────────────────────────────
            // Bug fix: datetime-local HTML envia "yyyy-MM-dd'T'HH:mm" (sem segundos).
            // LocalDateTime.parse() padrão exige segundos e lançava DateTimeParseException.
            try {
                LocalDateTime dataHora = LocalDateTime.parse(agendamento, DATETIME_LOCAL_FMT);
                Instant instant = dataHora.atZone(ZoneId.of("America/Sao_Paulo")).toInstant();

                if (instant.isBefore(Instant.now())) {
                    log.warn("Data de agendamento já passou: {}", dataHora);
                    return "Erro ao agendar: a data/hora informada já passou.";
                }

                taskScheduler.schedule(tarefa, instant);
                log.info("Disparo agendado para {} ({})", dataHora.format(EXIBICAO_FMT), instant);
                return "E-mail agendado para " + dataHora.format(EXIBICAO_FMT)
                        + " — " + destinatarios.size() + " destinatário(s).";

            } catch (Exception e) {
                log.error("Erro ao parsear data de agendamento '{}': {}", agendamento, e.getMessage());
                return "Erro ao agendar: data/hora inválida.";
            }

        } else {
            // ── IMEDIATO ──────────────────────────────────────────────────────
            // Bug fix: substituído new Thread().start() por taskScheduler.schedule(Instant.now())
            // para usar o pool gerenciado pelo Spring, evitar threads cruas e ter ciclo de vida controlado.
            taskScheduler.schedule(tarefa, Instant.now());
            log.info("Disparo imediato enfileirado via taskScheduler para {} destinatário(s).",
                    destinatarios.size());
            return "Disparo iniciado para " + destinatarios.size() + " destinatário(s). Acompanhe os logs.";
        }
    }
}
