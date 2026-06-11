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

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.mapper.CancelamentoMapper;
import umc.exs.dto.request.admin.CancelamentoRequest;
import umc.exs.dto.response.compras.CancelamentoResponse;
import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.model.enums.MotivoCategoria;
import umc.exs.service.cancelamento.CancelamentoService;

/**
 * Gerencia as solicitações de cancelamento de pedidos, tanto pelo cliente quanto pelo admin.
 * Expõe endpoints para solicitar, listar, aprovar, recusar e cancelar pedidos diretamente.
 */
@Slf4j
@RestController
public class CancelamentoController {

    private final CancelamentoService cancelamentoService;
    private final CancelamentoMapper mapper;

    public CancelamentoController(CancelamentoService cancelamentoService) {
        this.cancelamentoService = cancelamentoService;
        // Instancia o mapper via MapStruct sem precisar de injeção pelo Spring
        this.mapper = org.mapstruct.factory.Mappers.getMapper(CancelamentoMapper.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // CLIENTE: solicitar cancelamento
    // ─────────────────────────────────────────────────────────────────

    /**
     * Recebe a solicitação de cancelamento do cliente autenticado para um pedido específico.
     * Retorna 400 com mensagem de erro caso o pedido não possa ser cancelado neste momento.
     */
    @PostMapping("/api/pedidos/{pedidoId}/solicitar-cancelamento")
    public ResponseEntity<?> solicitar(
            @PathVariable Long pedidoId,
            @RequestBody @Valid CancelamentoRequest request,
            @AuthenticationPrincipal UserDetails user) {
        try {
            // Delega a regra de negócio ao service, passando o e-mail do usuário autenticado
            SolicitacaoCancelamento cancelamento = cancelamentoService.solicitarCancelamento(pedidoId,
                    user.getUsername(), request);
            CancelamentoResponse dto = mapper.toResponse(cancelamento);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Loga como warning pois é erro de validação de negócio, não erro do sistema
            log.warn("Falha ao solicitar cancelamento pedido={}: {}", pedidoId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN: listar / contar / aprovar / recusar
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retorna todas as solicitações de cancelamento existentes no sistema.
     * Acessível apenas por usuários com role ADMIN.
     */
    @GetMapping("/api/admin/cancelamentos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CancelamentoResponse>> listarTodas() {
        return ResponseEntity.ok(mapper.toResponseList(cancelamentoService.listarTodas()));
    }

    /**
     * Busca uma solicitação de cancelamento pelo seu ID.
     * Retorna 404 caso não seja encontrada.
     */
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

    /**
     * Lista somente as solicitações de cancelamento que ainda aguardam decisão do admin.
     * Utilizado para exibir a fila de pendências no painel administrativo.
     */
    @GetMapping("/api/admin/cancelamentos/pendentes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CancelamentoResponse>> listarPendentes() {
        return ResponseEntity.ok(mapper.toResponseList(cancelamentoService.listarPendentes()));
    }

    /** Badge counter: GET /api/admin/cancelamentos/pendentes/count */
    /**
     * Retorna a quantidade de cancelamentos pendentes para exibição no badge do menu admin.
     * Acessível apenas por ADMIN.
     */
    @GetMapping("/api/admin/cancelamentos/pendentes/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> contarPendentes() {
        return ResponseEntity.ok(cancelamentoService.contarPendentes());
    }

    /**
     * Aprova a solicitação de cancelamento informada, com comentário opcional do admin.
     * Retorna 400 caso o cancelamento já tenha sido processado ou não exista.
     */
    @PostMapping("/api/admin/cancelamentos/{id}/aprovar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aprovar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            // Extrai o comentário do corpo da requisição; aceita requisição sem body
            String comentario = body != null
                    ? body.get("comentario")
                    : null;
            return ResponseEntity.ok(cancelamentoService.aprovarCancelamento(id, comentario));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Falha ao aprovar cancelamento id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Recusa a solicitação de cancelamento informada, com comentário opcional do admin.
     * Retorna 400 caso o cancelamento já tenha sido processado ou não exista.
     */
    @PostMapping("/api/admin/cancelamentos/{id}/recusar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recusar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            // Extrai o comentário do corpo da requisição; aceita requisição sem body
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

    /**
     * Permite ao admin cancelar um pedido diretamente, sem necessidade de solicitação prévia do cliente.
     * Exige motivo categórico e justificativa com no mínimo 10 caracteres.
     */
    @PostMapping("/api/admin/pedidos/{pedidoId}/cancelar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> cancelarPeloAdmin(
            @PathVariable Long pedidoId,
            @RequestBody Map<String, String> body) {

        String motivoCategoriaStr = body.get("motivoCategoria");
        String justificativa = body.get("justificativa");

        // Valida tamanho mínimo da justificativa antes de processar
        if (justificativa == null || justificativa.trim().length() < 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Justificativa obrigatória (mínimo 10 caracteres)."));
        }

        try {
            // Usa DECISAO_ADMINISTRATIVA como categoria padrão caso não seja informada
            MotivoCategoria motivo = MotivoCategoria.valueOf(
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

    /**
     * Retorna todas as solicitações de cancelamento feitas pelo cliente autenticado.
     * Retorna 401 caso o usuário não esteja autenticado.
     */
    @GetMapping("/api/cancelamentos/meus")
    public ResponseEntity<?> meusCancelamentos(
            @AuthenticationPrincipal UserDetails user) {
        // Rejeita requisição se o token JWT não estiver presente ou for inválido
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
