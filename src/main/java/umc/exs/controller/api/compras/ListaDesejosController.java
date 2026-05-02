package umc.exs.controller.api.compras;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import umc.exs.DTOs.compra.ListaDesejosDTO;
import umc.exs.DTOs.compra.ListaDesejosRequestDTO;
import umc.exs.service.core.control.ListaDesejosService;

import java.util.List;

/**
 * API REST para gerenciamento da lista de desejos do cliente autenticado.
 * GET    /api/lista-desejos         → listar todos os desejos
 * POST   /api/lista-desejos         → adicionar ISBN à lista
 * DELETE /api/lista-desejos/{id}    → remover item da lista
 */
@RestController
@RequestMapping("/api/lista-desejos")
@RequiredArgsConstructor
public class ListaDesejosController {

    private final ListaDesejosService listaDesejosService;

    @GetMapping
    public ResponseEntity<List<ListaDesejosDTO>> listar(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(listaDesejosService.listarDesejos(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ListaDesejosDTO> adicionar(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody ListaDesejosRequestDTO dto) {
        if (user == null) return ResponseEntity.status(401).build();
        ListaDesejosDTO salvo = listaDesejosService.adicionarDesejo(user.getUsername(), dto.getIsbn());
        return ResponseEntity.status(201).body(salvo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        if (user == null) return ResponseEntity.status(401).build();
        listaDesejosService.removerDesejo(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
