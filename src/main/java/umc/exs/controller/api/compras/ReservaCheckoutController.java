package umc.exs.controller.api.compras;

import lombok.RequiredArgsConstructor;
import umc.exs.service.core.dashboard.ReservaCheckoutService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciamento de reservas temporárias de livros no checkout.
 * Permite reservar, liberar e consultar o status de reservas ativas por até 5 minutos.
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class ReservaCheckoutController {

    private final ReservaCheckoutService reservaService;

    /**
     * Reserva uma lista de livros para o cliente logado pelo prazo de 5 minutos.
     * Retorna motivo de falha caso algum livro já esteja reservado ou o limite seja excedido.
     */
    @PostMapping("/reservar")
    public ResponseEntity<Map<String, Object>> reservar(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        @SuppressWarnings("unchecked")
        List<Integer> idsRaw = (List<Integer>) body.get("livroIds");
        if (idsRaw == null || idsRaw.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("reservado", false, "mensagem", "Nenhum livro informado."));
        }

        List<Long> livroIds = idsRaw.stream()
                .map(i -> Long.valueOf(i))
                .toList();
        Map<String, Object> resultado = reservaService.reservar(livroIds, user.getUsername());
        return ResponseEntity.ok(resultado);
    }

    /**
     * Libera as reservas dos livros informados quando o cliente abandona o checkout.
     * Após 3 desistências, o cliente é bloqueado por 5 minutos para evitar abusos.
     */
    @DeleteMapping("/reservar")
    public ResponseEntity<Map<String, Object>> liberar(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        @SuppressWarnings("unchecked")
        List<Integer> idsRaw = (List<Integer>) body.get("livroIds");
        if (idsRaw == null || idsRaw.isEmpty()) {
            return ResponseEntity.ok(Map.of("liberado", false));
        }

        List<Long> livroIds = idsRaw.stream()
                .map(i -> Long.valueOf(i))
                .toList();
        Map<String, Object> resultado = reservaService.liberarReservas(livroIds, user.getUsername());
        return ResponseEntity.ok(resultado);
    }

    /** Consulta se o cliente possui reserva ativa para o livro e retorna o tempo restante em segundos. */
    @GetMapping("/reserva/status/{livroId}")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable Long livroId,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reservaService.statusReserva(livroId, user.getUsername()));
    }
}
