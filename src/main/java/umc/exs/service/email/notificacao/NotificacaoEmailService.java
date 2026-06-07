package umc.exs.service.email.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import umc.exs.dto.request.admin.EmailDisparoRequest;
import umc.exs.dto.response.email.EmailDestinatarioResponse;
import umc.exs.dto.response.email.EmailHistoricoResponse;
import umc.exs.model.entidades.foundation.EmailEnviado;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.EmailEnviadoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;

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
        private final EmailFacade emailFacade;
        private final TaskScheduler taskScheduler;
        private final EmailEnviadoRepository emailEnviadoRepository;

        private static final DateTimeFormatter DATETIME_LOCAL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        private static final DateTimeFormatter EXIBICAO_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

        // ── Filtrar destinatários ────────────────────────────────────────────────

        public List<EmailDestinatarioResponse> filtrarDestinatarios(String filtro, Integer limite) {
                int lim = (limite == null || limite <= 0) ? Integer.MAX_VALUE : limite;

                List<Cliente> clientes = clienteRepository.findAll();

                List<PontuacaoUsuario> pontuacoes = pontuacaoRepository.findAllWithCliente();
                Map<Long, Integer> xpMap = pontuacoes.stream()
                                .collect(Collectors.toMap(
                                                p -> p.getCliente().getId(),
                                                PontuacaoUsuario::getXpTotal));

                Comparator<Cliente> comparator = switch (filtro == null ? "todos" : filtro) {
                        case "ranking_maior" -> Comparator.comparingInt(
                                        (Cliente c) -> xpMap.getOrDefault(c.getId(), 0)).reversed();
                        case "ranking_menor" -> Comparator.comparingInt(
                                        (Cliente c) -> xpMap.getOrDefault(c.getId(), 0));
                        case "tokens_maior" -> Comparator.comparingDouble(
                                        (Cliente c) -> c.getSaldoTokens() == null ? 0.0 : c.getSaldoTokens())
                                        .reversed();
                        case "tokens_menor" -> Comparator.comparingDouble(
                                        (Cliente c) -> c.getSaldoTokens() == null ? 0.0 : c.getSaldoTokens());
                        case "mais_antigos" -> Comparator.comparing(
                                        (Cliente c) -> c.getDataCriacao() == null ? LocalDateTime.now()
                                                        : c.getDataCriacao());
                        case "mais_novos" -> Comparator.comparing(
                                        (Cliente c) -> c.getDataCriacao() == null ? LocalDateTime.now()
                                                        : c.getDataCriacao())
                                        .reversed();
                        default -> Comparator.comparing(Cliente::getId);
                };

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                return clientes.stream()
                                .sorted(comparator)
                                .limit(lim)
                                .map(c -> new EmailDestinatarioResponse(
                                                c.getId(),
                                                c.getNome(),
                                                c.getEmail(),
                                                c.getSaldoTokens() == null ? 0.0 : c.getSaldoTokens(),
                                                xpMap.getOrDefault(c.getId(), 0),
                                                c.getDataCriacao() != null ? c.getDataCriacao().format(fmt) : "—"))
                                .collect(Collectors.toList());
        }

        // ── Disparar ou agendar ──────────────────────────────────────────────────

        public String dispararOuAgendar(EmailDisparoRequest dto) {
                log.info("dispararOuAgendar — filtro='{}', limite={}, assunto='{}'",
                                dto.getFiltro(), dto.getLimite(), dto.getAssunto());

                List<EmailDestinatarioResponse> destinatarios;
                if ("emails_especificos".equals(dto.getFiltro())
                                && dto.getEmailsEspecificos() != null
                                && !dto.getEmailsEspecificos().isEmpty()) {
                        List<String> emailsAlvo = dto.getEmailsEspecificos().stream()
                                        .map(String::trim)
                                        .map(String::toLowerCase)
                                        .filter(e -> !e.isEmpty())
                                        .collect(Collectors.toList());
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        destinatarios = clienteRepository.findAll().stream()
                                        .filter(c -> emailsAlvo.contains(c.getEmail().toLowerCase()))
                                        .map(c -> new EmailDestinatarioResponse(
                                                        c.getId(), c.getNome(), c.getEmail(),
                                                        c.getSaldoTokens() == null ? 0.0 : c.getSaldoTokens(),
                                                        0,
                                                        c.getDataCriacao() != null ? c.getDataCriacao().format(fmt) : "—"))
                                        .collect(Collectors.toList());
                } else {
                        destinatarios = filtrarDestinatarios(dto.getFiltro(), dto.getLimite());
                }

                if (destinatarios.isEmpty()) {
                        log.warn("Nenhum destinatário encontrado para filtro='{}'", dto.getFiltro());
                        return "Nenhum destinatário encontrado para o filtro selecionado.";
                }

                log.info("{} destinatário(s) selecionado(s).", destinatarios.size());

                // Runnable usa EmailHtmlBuilder + enviarHtml() igual aos outros emails do
                // sistema
                Runnable tarefa = () -> {
                        log.info(">>> Iniciando disparo: assunto='{}', {} destinatário(s)",
                                        dto.getAssunto(), destinatarios.size());
                        int enviados = 0, erros = 0;
                        for (EmailDestinatarioResponse dest : destinatarios) {
                                try {
                                        emailFacade.sendHtmlSafe(
                                                        dest.getEmail(),
                                                        dto.getAssunto(),
                                                        EmailHtmlBuilder.comunicadoAdmin(dest.getNome(),
                                                                        dto.getCorpo()));
                                        enviados++;
                                        log.debug(" Enviado para: {}", dest.getEmail());
                                } catch (Exception e) {
                                        erros++;
                                        log.error(" Falha ao enviar para {} — {}: {}",
                                                        dest.getEmail(), e.getClass().getSimpleName(), e.getMessage());
                                }
                        }
                        log.info("<<< Disparo concluído — enviados: {}, erros: {}", enviados, erros);
                };

                String agendamento = dto.getAgendamento();
                if (agendamento != null && !agendamento.isBlank()) {
                        // ── AGENDADO ──────────────────────────────────────────────────────
                        try {
                                LocalDateTime dataHora = LocalDateTime.parse(agendamento, DATETIME_LOCAL_FMT);
                                Instant instant = dataHora.atZone(ZoneId.of("America/Sao_Paulo")).toInstant();

                                if (instant.isBefore(Instant.now())) {
                                        log.warn("Data de agendamento já passou: {}", dataHora);
                                        return "Erro ao agendar: a data/hora informada já passou.";
                                }

                                taskScheduler.schedule(tarefa, instant);
                                log.info("Disparo agendado para {} ({})", dataHora.format(EXIBICAO_FMT), instant);

                                emailEnviadoRepository.save(EmailEnviado.builder()
                                                .assunto(dto.getAssunto())
                                                .corpo(dto.getCorpo())
                                                .filtro(dto.getFiltro())
                                                .quantidadeDestinatarios(destinatarios.size())
                                                .dataRegistro(LocalDateTime.now())
                                                .agendadoPara(dataHora)
                                                .tipo("AGENDADO")
                                                .build());

                                return "E-mail agendado para " + dataHora.format(EXIBICAO_FMT)
                                                + " — " + destinatarios.size() + " destinatário(s).";

                        } catch (Exception e) {
                                log.error("Erro ao parsear data de agendamento '{}': {}", agendamento, e.getMessage());
                                return "Erro ao agendar: data/hora inválida.";
                        }

                } else {
                        // ── IMEDIATO ──────────────────────────────────────────────────────
                        taskScheduler.schedule(tarefa, Instant.now());
                        log.info("Disparo imediato enfileirado via taskScheduler para {} destinatário(s).",
                                        destinatarios.size());

                        emailEnviadoRepository.save(EmailEnviado.builder()
                                        .assunto(dto.getAssunto())
                                        .corpo(dto.getCorpo())
                                        .filtro(dto.getFiltro())
                                        .quantidadeDestinatarios(destinatarios.size())
                                        .dataRegistro(LocalDateTime.now())
                                        .agendadoPara(null)
                                        .tipo("IMEDIATO")
                                        .build());

                        return "Disparo iniciado para " + destinatarios.size() + " destinatário(s). Acompanhe os logs.";
                }
        }

        // ── Histórico de e-mails disparados ─────────────────────────────────────

        public List<EmailHistoricoResponse> listarHistorico() {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
                return emailEnviadoRepository.findAllByOrderByDataRegistroDesc().stream()
                                .map(e -> new EmailHistoricoResponse(
                                                e.getId(),
                                                e.getAssunto(),
                                                e.getCorpo(),
                                                e.getFiltro(),
                                                e.getQuantidadeDestinatarios(),
                                                e.getDataRegistro() != null ? e.getDataRegistro().format(fmt) : "—",
                                                e.getAgendadoPara() != null ? e.getAgendadoPara().format(fmt) : null,
                                                e.getTipo()))
                                .collect(Collectors.toList());
        }
}