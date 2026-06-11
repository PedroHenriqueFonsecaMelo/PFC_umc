package umc.exs.service.core.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.entidades.foundation.ReservaCheckout;
import umc.exs.repository.negocios.ReservaCheckoutRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço responsável pela reserva temporária de livros durante o checkout.
 * Bloqueia livros por 5 minutos para o comprador e libera automaticamente se expirar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaCheckoutService {

    private final ReservaCheckoutRepository reservaRepo;
    private final ClienteRepository clienteRepo;
    private final AppLogger appLogger;

    private static final int DURACAO_RESERVA_MIN = 5;
    private static final int LIMITE_TENTATIVAS = 3;
    private static final int BLOQUEIO_MIN = 5;
    private static final int LIMITE_LIVROS = 5;

    /**
     * Reserva um conjunto de livros para o cliente durante o checkout por 5 minutos.
     * Bloqueia o livro para outros compradores e impede reservas duplicadas ativas.
     */
    @Transactional
    public Map<String, Object> reservar(List<Long> livroIds, String emailUsuario) {

        if (livroIds.size() > LIMITE_LIVROS) {
            return Map.of(
                    "reservado", false,
                    "motivo", "LIMITE_EXCEDIDO",
                    "mensagem", "Limite de " + LIMITE_LIVROS + " livros");
        }

        Long clienteId = resolverClienteId(emailUsuario);

        if (clienteId == null) {
            return Map.of("reservado", false, "motivo", "NAO_AUTENTICADO");
        }

        LocalDateTime agora = LocalDateTime.now();

        for (Long livroId : livroIds) {

            Optional<ReservaCheckout> existente =
                    reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

            if (existente.isPresent()) {

                ReservaCheckout r = existente.get();

                if (r.getBloqueadoAte() != null &&
                        agora.isBefore(r.getBloqueadoAte())) {

                    appLogger.error(
                            AcaoAuditoria.GENERICO,
                            clienteId,
                            emailUsuario,
                            "Tentativa bloqueada livroId=" + livroId);

                    return Map.of(
                            "reservado", false,
                            "motivo", "BLOQUEADO",
                            "bloqueadoAte", r.getBloqueadoAte().toString());
                }
            }

            Optional<ReservaCheckout> outro =
                    reservaRepo.findReservaAtivaDeOutro(livroId, clienteId, agora);

            if (outro.isPresent()) {

                appLogger.info(
                        AcaoAuditoria.GENERICO,
                        clienteId,
                        emailUsuario,
                        "Livro indisponível livroId=" + livroId);

                return Map.of(
                        "reservado", false,
                        "motivo", "INDISPONIVEL",
                        "livroId", livroId);
            }
        }

        // RESERVA — livro bloqueado por 5 minutos para o comprador enquanto ele finaliza o pedido
        LocalDateTime expira = agora.plusMinutes(DURACAO_RESERVA_MIN);

        for (Long livroId : livroIds) {

            Optional<ReservaCheckout> existente =
                    reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

            if (existente.isPresent()) {

                ReservaCheckout r = existente.get();
                r.setReservadoEm(agora);
                r.setExpiraEm(expira);
                reservaRepo.save(r);

            } else {

                reservaRepo.save(ReservaCheckout.builder()
                        .livroId(livroId)
                        .clienteId(clienteId)
                        .reservadoEm(agora)
                        .expiraEm(expira)
                        .tentativas(0)
                        .build());
            }
        }

        appLogger.success(
                AcaoAuditoria.PAGAMENTO_INTENCAO_REGISTRADA,
                clienteId,
                emailUsuario,
                "Reserva criada");

        log.info("RESERVA_CRIADA clienteId={} livros={}", clienteId, livroIds.size());

        return Map.of(
                "reservado", true,
                "expiraEm", expira.toString(),
                "duracaoSegundos", DURACAO_RESERVA_MIN * 60);
    }

    /**
     * Libera as reservas dos livros quando o cliente abandona o checkout.
     * Após 3 desistências, bloqueia o cliente por 5 minutos para evitar abuso.
     */
    @Transactional
    public Map<String, Object> liberarReservas(List<Long> livroIds, String emailUsuario) {

        Long clienteId = resolverClienteId(emailUsuario);

        if (clienteId == null) {
            return Map.of("liberado", false);
        }

        LocalDateTime agora = LocalDateTime.now();

        for (Long livroId : livroIds) {

            Optional<ReservaCheckout> opt =
                    reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

            if (opt.isPresent()) {

                ReservaCheckout r = opt.get();

                int tentativas = r.getTentativas() + 1;
                r.setTentativas(tentativas);

                // BLOQUEIO — após 3 desistências consecutivas, cliente fica bloqueado por 5 minutos
                if (tentativas >= LIMITE_TENTATIVAS) {

                    r.setBloqueadoAte(agora.plusMinutes(BLOQUEIO_MIN));
                    r.setTentativas(0);
                    r.setExpiraEm(agora.minusSeconds(1));

                    reservaRepo.save(r);

                } else {
                    reservaRepo.delete(r);
                }
            }
        }

        appLogger.info(
                AcaoAuditoria.GENERICO,
                clienteId,
                emailUsuario,
                "Reservas liberadas");

        log.info("RESERVA_LIBERADA clienteId={}", clienteId);

        return Map.of("liberado", true);
    }

    /** Consulta se o cliente possui uma reserva ativa para o livro e retorna o tempo restante em segundos. */
    @Transactional(readOnly = true)
    public Map<String, Object> statusReserva(Long livroId, String emailUsuario) {

        Long clienteId = resolverClienteId(emailUsuario);

        if (clienteId == null) {
            return Map.of("ativa", false);
        }

        LocalDateTime agora = LocalDateTime.now();

        Optional<ReservaCheckout> opt =
                reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

        if (opt.isEmpty()) {
            return Map.of("ativa", false);
        }

        ReservaCheckout r = opt.get();

        if (agora.isAfter(r.getExpiraEm())) {
            return Map.of("ativa", false);
        }

        long segundos =
                java.time.Duration.between(agora, r.getExpiraEm()).getSeconds();

        return Map.of(
                "ativa", true,
                "segundosRestantes", segundos,
                "expiraEm", r.getExpiraEm().toString());
    }

    /** Job agendado que roda a cada 60 segundos e remove reservas com prazo expirado do banco. */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void limparReservasExpiradas() {

        reservaRepo.deleteExpiradas(LocalDateTime.now());

        log.debug("RESERVAS_EXPIRADAS_LIMPAS");
    }

    /** Busca o ID do cliente pelo e-mail; retorna null se não encontrado (sessão inválida). */
    private Long resolverClienteId(String email) {

        return clienteRepo.findByEmail(email)
                .map(c -> c.getId())
                .orElse(null);
    }
}