package umc.exs.controller.api.control;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.foundation.NotificacaoDashboard;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.NotificacaoDashboardRepository;
import umc.exs.repository.usuario.ClienteRepository;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoDashboardRepository notificacaoRepository;
    private final ClienteRepository clienteRepository;

    /** Lista todas as notificações do usuário (não lidas primeiro). */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        List<Map<String, Object>> notificacoes = notificacaoRepository
                .findByClienteIdOrderByDataCriacaoDesc(cliente.getId())
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(notificacoes);
    }

    /** Lista notificações não lidas + total. */
    @GetMapping("/nao-lidas")
    public ResponseEntity<?> naoLidas(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        long total = notificacaoRepository.countByClienteIdAndLidaFalse(cliente.getId());
        List<Map<String, Object>> lista = notificacaoRepository
                .findByClienteIdAndLidaFalseOrderByDataCriacaoDesc(cliente.getId())
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("total", total, "notificacoes", lista));
    }

    /** Marca uma notificação como lida. */
    @PatchMapping("/{id}/lida")
    public ResponseEntity<?> marcarLida(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        NotificacaoDashboard notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        if (!notificacao.getCliente().getId().equals(cliente.getId())) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado."));
        }

        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);
        return ResponseEntity.ok(Map.of("mensagem", "Notificação marcada como lida."));
    }

    private Map<String, Object> toMap(NotificacaoDashboard n) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", n.getId());
        map.put("mensagem", n.getMensagem());
        map.put("dataCriacao", n.getDataCriacao().toString());
        map.put("lida", n.isLida());
        if (n.getLink() != null) map.put("link", n.getLink());
        return map;
    }
}
