package umc.exs.service.core.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.exs.model.entidades.foundation.ReservaCheckout;
import umc.exs.repository.negocios.ReservaCheckoutRepository;
import umc.exs.repository.usuario.ClienteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaCheckoutService {

    private final ReservaCheckoutRepository reservaRepo;
    private final ClienteRepository clienteRepo;

    private static final int DURACAO_RESERVA_MIN = 5;
    private static final int LIMITE_TENTATIVAS = 3;
    private static final int BLOQUEIO_MIN = 5;
    private static final int LIMITE_LIVROS = 5;

    @Transactional
    public Map<String, Object> reservar(List<Long> livroIds, String emailUsuario) {
        // Limite de 5 livros por checkout
        if (livroIds.size() > LIMITE_LIVROS) {
            return Map.of(
                    "reservado", false,
                    "motivo", "LIMITE_EXCEDIDO",
                    "mensagem",
                    "Limite de " + LIMITE_LIVROS + " livros por compra. Remova alguns itens para continuar.");
        }

        Long clienteId = resolverClienteId(emailUsuario);
        if (clienteId == null)
            return Map.of("reservado", false, "motivo", "NAO_AUTENTICADO");

        LocalDateTime agora = LocalDateTime.now();

        for (Long livroId : livroIds) {
            // Verifica bloqueio anti-abuso
            Optional<ReservaCheckout> reservaExistente = reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

            if (reservaExistente.isPresent()) {
                ReservaCheckout r = reservaExistente.get();
                if (r.getBloqueadoAte() != null && agora.isBefore(r.getBloqueadoAte())) {
                    long segundosRestantes = java.time.Duration.between(agora, r.getBloqueadoAte()).getSeconds();
                    long minutos = segundosRestantes / 60;
                    long segundos = segundosRestantes % 60;
                    return Map.of(
                            "reservado", false,
                            "motivo", "BLOQUEADO",
                            "mensagem", String.format(
                                    "Você pode tentar novamente em %d:%02d.", minutos, segundos),
                            "bloqueadoAte", r.getBloqueadoAte().toString());
                }
            }

            // Verifica se outro usuário tem reserva ativa
            Optional<ReservaCheckout> reservaOutro = reservaRepo.findReservaAtivaDeOutro(livroId, clienteId, agora);

            if (reservaOutro.isPresent()) {
                long segundosRestantes = java.time.Duration.between(
                        agora, reservaOutro.get().getExpiraEm()).getSeconds();
                return Map.of(
                        "reservado", false,
                        "motivo", "INDISPONIVEL",
                        "livroId", livroId,
                        "mensagem", "Um ou mais livros estão temporariamente reservados. Tente novamente em breve.",
                        "segundosRestantes", segundosRestantes);
            }
        }

        // Cria ou renova reservas para todos os livros
        LocalDateTime expira = agora.plusMinutes(DURACAO_RESERVA_MIN);
        for (Long livroId : livroIds) {
            Optional<ReservaCheckout> existente = reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

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

        return Map.of(
                "reservado", true,
                "expiraEm", expira.toString(),
                "duracaoSegundos", DURACAO_RESERVA_MIN * 60);
    }

    @Transactional
    public Map<String, Object> liberarReservas(List<Long> livroIds, String emailUsuario) {
        Long clienteId = resolverClienteId(emailUsuario);
        if (clienteId == null)
            return Map.of("liberado", false);

        LocalDateTime agora = LocalDateTime.now();

        for (Long livroId : livroIds) {
            Optional<ReservaCheckout> opt = reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

            if (opt.isPresent()) {
                ReservaCheckout r = opt.get();
                int novasTentativas = r.getTentativas() + 1;
                r.setTentativas(novasTentativas);

                if (novasTentativas >= LIMITE_TENTATIVAS) {
                    // Bloqueia por 5 minutos e reseta contador
                    r.setBloqueadoAte(agora.plusMinutes(BLOQUEIO_MIN));
                    r.setTentativas(0);
                    r.setExpiraEm(agora.minusSeconds(1)); // expira imediatamente
                    reservaRepo.save(r);
                } else {
                    reservaRepo.delete(r);
                }
            }
        }

        return Map.of("liberado", true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statusReserva(Long livroId, String emailUsuario) {
        Long clienteId = resolverClienteId(emailUsuario);
        if (clienteId == null)
            return Map.of("ativa", false);

        LocalDateTime agora = LocalDateTime.now();
        Optional<ReservaCheckout> opt = reservaRepo.findByLivroIdAndClienteId(livroId, clienteId);

        if (opt.isEmpty())
            return Map.of("ativa", false);

        ReservaCheckout r = opt.get();
        if (agora.isAfter(r.getExpiraEm()))
            return Map.of("ativa", false);

        long segundosRestantes = java.time.Duration.between(agora, r.getExpiraEm()).getSeconds();
        return Map.of(
                "ativa", true,
                "segundosRestantes", segundosRestantes,
                "expiraEm", r.getExpiraEm().toString());
    }

    // Cron: limpa reservas expiradas a cada 1 minuto
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void limparReservasExpiradas() {
        reservaRepo.deleteExpiradas(LocalDateTime.now());
        log.debug("Reservas expiradas limpas.");
    }

    private Long resolverClienteId(String email) {
        return clienteRepo.findByEmail(email)
                .map(c -> c.getId())
                .orElse(null);
    }
}
