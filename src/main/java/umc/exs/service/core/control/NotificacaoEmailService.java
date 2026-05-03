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

    public String dispararOuAgendar(EmailDisparoDTO dto) {
        List<EmailDestinatarioDTO> destinatarios = filtrarDestinatarios(dto.getFiltro(), dto.getLimite());

        if (destinatarios.isEmpty()) {
            return "Nenhum destinatário encontrado para o filtro selecionado.";
        }

        Runnable tarefa = () -> {
            int enviados = 0, erros = 0;
            for (EmailDestinatarioDTO dest : destinatarios) {
                try {
                    emailService.enviar(dest.getEmail(), dto.getAssunto(), dto.getCorpo());
                    enviados++;
                    log.info("E-mail enviado para: {}", dest.getEmail());
                } catch (Exception e) {
                    erros++;
                    log.error("Erro ao enviar para {}: {}", dest.getEmail(), e.getMessage());
                }
            }
            log.info("Disparo concluído. Enviados: {}, Erros: {}", enviados, erros);
        };

        String agendamento = dto.getAgendamento();
        if (agendamento != null && !agendamento.isBlank()) {
            try {
                LocalDateTime dataHora = LocalDateTime.parse(agendamento);
                Instant instant = dataHora.atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
                taskScheduler.schedule(tarefa, instant);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
                return "E-mail agendado para " + dataHora.format(fmt) + " — " + destinatarios.size() + " destinatário(s).";
            } catch (Exception e) {
                return "Erro ao agendar: data/hora inválida.";
            }
        } else {
            new Thread(tarefa).start();
            return "Disparo iniciado para " + destinatarios.size() + " destinatário(s). Acompanhe os logs.";
        }
    }
}
