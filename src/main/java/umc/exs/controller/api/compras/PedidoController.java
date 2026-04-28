package umc.exs.controller.api.compras;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.compra.PedidoDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.PedidoService;

/**
 * API REST para consulta de pedidos do cliente autenticado.
 * GET /api/pedidos/pendentes → compras em andamento
 * GET /api/pedidos/concluidos → compras entregues
 * GET /api/pedidos/todos → histórico completo
 */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;

    @GetMapping("/pendentes")
    public ResponseEntity<?> listarPendentes(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        Long id = resolverId(user.getUsername());
        List<PedidoDTO> lista = pedidoService.listarPendentes(id);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/concluidos")
    public ResponseEntity<?> listarConcluidos(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        Long id = resolverId(user.getUsername());
        List<PedidoDTO> lista = pedidoService.listarConcluidos(id);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/todos")
    public ResponseEntity<?> listarTodos(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        Long id = resolverId(user.getUsername());
        List<PedidoDTO> lista = pedidoService.listarPorCliente(id);
        return ResponseEntity.ok(lista);
    }

    private Long resolverId(String email) {
        return clienteService.buscarClientePorEmail(email)
                .map(ClienteDTO::getId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }
}
