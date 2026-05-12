package umc.exs.controller.api.compras;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dtos.cancelamento.RespostaAdminDTO;
import umc.exs.dtos.cancelamento.SolicitacaoCancelamentoRequestDTO;
import umc.exs.dtos.cancelamento.SolicitacaoCancelamentoResponseDTO;
import umc.exs.service.cancelamento.CancelamentoService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CancelamentoController {

    private final CancelamentoService cancelamentoService;

    // ─────────────────────────────────────────────────────────────────
    // CLIENTE: solicitar cancelamento
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/api/pedidos/{pedidoId}/solicitar-cancelamento")
    public ResponseEntity<?> solicitar(
            @PathVariable Long pedidoId,
            @RequestBody SolicitacaoCancelamentoRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            SolicitacaoCancelamentoResponseDTO dto =
                    cancelamentoService.solicitarCancelamento(pedidoId, user.getUsername(), request);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Falha ao solicitar cancelamento pedido={}: {}", pedidoId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN: listar / contar / aprovar / recusar
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/cancelamentos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SolicitacaoCancelamentoResponseDTO>> listarTodas() {
        return ResponseEntity.ok(cancelamentoService.listarTodas());
    }

    @GetMapping("/api/admin/cancelamentos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(cancelamentoService.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/admin/cancelamentos/pendentes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SolicitacaoCancelamentoResponseDTO>> listarPendentes() {
        return ResponseEntity.ok(cancelamentoService.listarPendentes());
    }

    /** Badge counter: GET /api/admin/cancelamentos/pendentes/count */
    @GetMapping("/api/admin/cancelamentos/pendentes/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> contarPendentes() {
        return ResponseEntity.ok(cancelamentoService.contarPendentes());
    }

    @PostMapping("/api/admin/cancelamentos/{id}/aprovar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aprovar(
            @PathVariable Long id,
            @RequestBody(required = false) RespostaAdminDTO body) {
        try {
            String comentario = (body != null) ? body.getComentarioAdmin() : null;
            return ResponseEntity.ok(cancelamentoService.aprovarCancelamento(id, comentario));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Falha ao aprovar cancelamento id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/admin/cancelamentos/{id}/recusar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recusar(
            @PathVariable Long id,
            @RequestBody(required = false) RespostaAdminDTO body) {
        try {
            String comentario = (body != null) ? body.getComentarioAdmin() : null;
            return ResponseEntity.ok(cancelamentoService.recusarCancelamento(id, comentario));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Falha ao recusar cancelamento id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
