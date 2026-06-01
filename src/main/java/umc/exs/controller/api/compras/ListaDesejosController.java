package umc.exs.controller.api.compras;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import umc.exs.dto.mapper.ListaDesejosMapper;
import umc.exs.dto.response.cliente.ListaDesejosResponse;
import umc.exs.service.core.dashboard.ListaDesejosService;
import java.util.List;
import java.util.Map;

/**
 * API REST para gerenciamento da lista de desejos do cliente autenticado.
 * GET /api/lista-desejos → listar todos os desejos
 * POST /api/lista-desejos → adicionar ISBN à lista
 * DELETE /api/lista-desejos/{id} → remover item da lista
 */
@RestController
@RequestMapping("/api/lista-desejos")
@RequiredArgsConstructor
public class ListaDesejosController {

    private final ListaDesejosService listaDesejosService;
    private final ListaDesejosMapper mapper;

    @GetMapping
    public ResponseEntity<List<ListaDesejosResponse>> listar(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(mapper.toDTOList(listaDesejosService.listarDesejos(user.getUsername())));
    }

    @PostMapping
    public ResponseEntity<ListaDesejosResponse> adicionar(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody Map<String, String> dto) {
        if (user == null)
            return ResponseEntity.status(401).build();

        String isbn = dto != null ? dto.get("isbn") : null;
        return ResponseEntity.status(201)
                .body(mapper.toDTO(listaDesejosService.adicionarDesejo(user.getUsername(), isbn)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        if (user == null)
            return ResponseEntity.status(401).build();
        listaDesejosService.removerDesejo(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ativa ou desativa a pré-reserva automática para um item da lista de desejos.
     */
    @PatchMapping("/{id}/pre-reserva")
    public ResponseEntity<?> togglePreReserva(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        if (user == null)
            return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(mapper.toDTO(listaDesejosService.togglePreReserva(user.getUsername(), id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage()));
        }
    }
}
