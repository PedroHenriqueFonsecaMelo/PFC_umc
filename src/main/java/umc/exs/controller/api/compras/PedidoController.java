package umc.exs.controller.api.compras;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.mapper.PedidoMapper;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final PedidoMapper pedidoMapper;

    @GetMapping("/pendentes")
    public ResponseEntity<?> listarPendentes(@AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        Long id = resolverId(user.getUsername());

        var lista = pedidoService.listarPendentes(id);

        return ResponseEntity.ok(
                pedidoMapper.toResponseList(lista)
        );
    }

    @GetMapping("/concluidos")
    public ResponseEntity<?> listarConcluidos(@AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        Long id = resolverId(user.getUsername());

        var lista = pedidoService.listarConcluidos(id);

        return ResponseEntity.ok(
                pedidoMapper.toResponseList(lista)
        );
    }

    @GetMapping("/todos")
    public ResponseEntity<?> listarTodos(@AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        Long id = resolverId(user.getUsername());

        var lista = pedidoService.listarPorCliente(id);

        return ResponseEntity.ok(
                pedidoMapper.toResponseList(lista)
        );
    }

    @GetMapping("/{pedidoId}")
    public ResponseEntity<?> buscarPorId(
            @PathVariable Long pedidoId,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        Long clienteId = resolverId(user.getUsername());

        return pedidoService.buscarPorIdEComprador(pedidoId, clienteId)
                .map(pedidoMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Long resolverId(String email) {

        return clienteService.buscarClientePorEmail(email)
                .map(Cliente::getId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }
}