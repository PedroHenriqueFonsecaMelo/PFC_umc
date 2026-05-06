package umc.exs.controller.api.control;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.foundation.SessaoAtiva;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.cliente.SessaoService;

@RestController
@RequestMapping("/api/sessoes")
@RequiredArgsConstructor
public class SessaoController {

    private final SessaoService sessaoService;
    private final ClienteRepository clienteRepository;

    /** Lista sessões ativas do usuário logado. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarSessoes(
            @AuthenticationPrincipal UserDetails user) {
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        List<Map<String, Object>> sessoes = sessaoService.listarSessoesAtivas(cliente.getId())
                .stream()
                .map(s -> toMap(s))
                .collect(Collectors.toList());

        return ResponseEntity.ok(sessoes);
    }

    /** Encerra remotamente uma sessão específica. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> encerrarSessao(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        try {
            sessaoService.encerrarSessaoPorId(id, cliente.getId());
            return ResponseEntity.ok(Map.of("mensagem", "Sessão encerrada com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        }
    }

    /** Encerra todas as sessões ativas do usuário. */
    @DeleteMapping
    public ResponseEntity<?> encerrarTodasSessoes(@AuthenticationPrincipal UserDetails user) {
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        sessaoService.encerrarTodasSessoes(cliente.getId());
        return ResponseEntity.ok(Map.of("mensagem", "Todas as sessões foram encerradas."));
    }

    private Map<String, Object> toMap(SessaoAtiva s) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("dataLogin", s.getDataLogin().toString());
        map.put("ip", s.getIp() != null ? s.getIp() : "");
        map.put("userAgent", s.getUserAgent() != null ? s.getUserAgent() : "");
        map.put("ativa", s.isAtiva());
        return map;
    }
}
