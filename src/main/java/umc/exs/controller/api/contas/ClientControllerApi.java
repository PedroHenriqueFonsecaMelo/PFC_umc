package umc.exs.controller.api.contas;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.service.core.cliente.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClientControllerApi {

    private final ClienteService clienteService;

    /** Retorna dados do perfil para atualizar saldo/nome via JS */
    @GetMapping("/meu-perfil")
    public ResponseEntity<ClienteDTO> getPerfilJson(@AuthenticationPrincipal UserDetails user) {
        return clienteService.buscarClientePorEmail(user.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Retorna histórico de transações em JSON */
    @GetMapping("/transacoes")
    public ResponseEntity<List<Transacao>> getHistorico(@AuthenticationPrincipal UserDetails user) {
        Long id = clienteService.buscarClientePorEmail(user.getUsername()).map(ClienteDTO::getId).orElseThrow();
        return ResponseEntity.ok(clienteService.listarHistoricoTransacoes(id));
    }

    /** Endpoint para verificar se o e-mail já existe (validação em tempo real no form) */
    @GetMapping("/verificar-email")
    public ResponseEntity<Boolean> verificarEmail(@RequestParam String email) {
        return ResponseEntity.ok(clienteService.buscarClientePorEmail(email).isPresent());
    }
}