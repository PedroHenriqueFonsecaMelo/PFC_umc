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
import umc.exs.dto.mapper.CancelamentoMapper;
import umc.exs.dto.request.admin.CancelamentoRequest;
import umc.exs.dto.response.compras.CancelamentoResponse;
import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.service.cancelamento.CancelamentoService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CancelamentoController {

    private final CancelamentoService cancelamentoService;
    private final CancelamentoMapper mapper;

    // ─────────────────────────────────────────────────────────────────
    // CLIENTE: solicitar cancelamento
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/api/pedidos/{pedidoId}/solicitar-cancelamento")
    public ResponseEntity<?> solicitar(
            @PathVariable Long pedidoId,
            @RequestBody CancelamentoRequest request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            SolicitacaoCancelamento cancelamento = cancelamentoService.solicitarCancelamento(pedidoId,
                    user.getUsername(), request);
            CancelamentoResponse dto = mapper.toResponse(cancelamento);
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
    public ResponseEntity<List<CancelamentoResponse>> listarTodas() {
        return ResponseEntity.ok(mapper.toResponseList(cancelamentoService.listarTodas()));
    }

    @GetMapping("/api/admin/cancelamentos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            SolicitacaoCancelamento cancelamento = cancelamentoService.buscarPorId(id);
            CancelamentoResponse dto = mapper.toResponse(cancelamento);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/admin/cancelamentos/pendentes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CancelamentoResponse>> listarPendentes() {
        return ResponseEntity.ok(mapper.toResponseList(cancelamentoService.listarPendentes()));
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
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comentario = body != null
                    ? body.get("comentario")
                    : null;
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
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comentario = body != null
                    ? body.get("comentario")
                    : null;
            return ResponseEntity.ok(cancelamentoService.recusarCancelamento(id, comentario));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Falha ao recusar cancelamento id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN: cancelar pedido diretamente (com motivo + justificativa)
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/api/admin/pedidos/{pedidoId}/cancelar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> cancelarPeloAdmin(
            @PathVariable Long pedidoId,
            @RequestBody Map<String, String> body) {

        String motivoCategoriaStr = body.get("motivoCategoria");
        String justificativa = body.get("justificativa");

        if (justificativa == null || justificativa.trim().length() < 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Justificativa obrigatória (mínimo 10 caracteres)."));
        }

        try {
            umc.exs.model.enums.MotivoCategoria motivo = umc.exs.model.enums.MotivoCategoria.valueOf(
                    motivoCategoriaStr != null ? motivoCategoriaStr : "DECISAO_ADMINISTRATIVA");

            var resultado = cancelamentoService.cancelarPeloAdmin(
                    pedidoId, motivo, justificativa.trim());
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CLIENTE: listar seus cancelamentos
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/cancelamentos/meus")
    public ResponseEntity<?> meusCancelamentos(
            @AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        try {
            var lista = cancelamentoService.listarCancelamentosCliente(user.getUsername());
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
