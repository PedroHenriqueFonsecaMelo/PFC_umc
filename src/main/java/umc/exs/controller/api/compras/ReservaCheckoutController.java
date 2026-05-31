package umc.exs.controller.api.compras;

import lombok.RequiredArgsConstructor;
import umc.exs.service.core.dashboard.ReservaCheckoutService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class ReservaCheckoutController {

    private final ReservaCheckoutService reservaService;

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

    @GetMapping("/reserva/status/{livroId}")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable Long livroId,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reservaService.statusReserva(livroId, user.getUsername()));
    }
}
