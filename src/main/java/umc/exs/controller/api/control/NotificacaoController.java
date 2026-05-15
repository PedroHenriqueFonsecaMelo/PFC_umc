package umc.exs.controller.api.control;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // Constante para evitar duplicação de literais
    private static final String CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado";

    /** Lista todas as notificações do usuário (não lidas primeiro). */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        // Uso de .toList() para Java 16+ (Lista imutável)
        List<Map<String, Object>> notificacoes = notificacaoRepository
                .findByClienteIdOrderByDataCriacaoDesc(cliente.getId())
                .stream()
                .map(this::toMap)
                .toList();

        return ResponseEntity.ok(notificacoes);
    }

    /** Lista notificações não lidas + total. */
    @GetMapping("/nao-lidas")
    public ResponseEntity<Map<String, Object>> naoLidas(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        long total = notificacaoRepository.countByClienteIdAndLidaFalse(cliente.getId());
        
        List<Map<String, Object>> lista = notificacaoRepository
                .findByClienteIdAndLidaFalseOrderByDataCriacaoDesc(cliente.getId())
                .stream()
                .map(this::toMap)
                .toList();

        return ResponseEntity.ok(Map.of("total", total, "notificacoes", lista));
    }

    /** Marca uma notificação como lida. */
    @PatchMapping("/{id}/lida")
    public ResponseEntity<Map<String, String>> marcarLida(
            @PathVariable @NonNull Long id,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        NotificacaoDashboard notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        // Null Safety: Garantindo que o ID do cliente não é nulo antes da comparação
        Long clienteIdLogado = Objects.requireNonNull(cliente.getId(), "ID do cliente logado está nulo");
        Long clienteIdNotificacao = Objects.requireNonNull(notificacao.getCliente().getId(), "ID do dono da notificação está nulo");

        if (!clienteIdNotificacao.equals(clienteIdLogado)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", "Acesso negado."));
        }

        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);
        return ResponseEntity.ok(Map.of("mensagem", "Notificação marcada como lida."));
    }

    /** Cria uma notificação para o usuário autenticado (usado pelo frontend para alertas de preço). */
    @PostMapping
    public ResponseEntity<Map<String, Object>> criar(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String mensagem = body.get("mensagem");
        if (mensagem == null || mensagem.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Mensagem obrigatória."));
        }

        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        NotificacaoDashboard notif = NotificacaoDashboard.builder()
                .cliente(cliente)
                .mensagem(mensagem.length() > 255 ? mensagem.substring(0, 255) : mensagem)
                .dataCriacao(java.time.LocalDateTime.now())
                .link(body.get("link"))
                .build();

        notificacaoRepository.save(notif);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(notif));
    }

    /** Marca todas as notificações do usuário como lidas. */
    @PostMapping("/marcar-lidas")
    @Transactional
    public ResponseEntity<Map<String, Object>> marcarTodas(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        int atualizadas = notificacaoRepository.marcarTodasLidas(cliente.getId());
        return ResponseEntity.ok(Map.of("atualizadas", atualizadas));
    }

    /** Remove uma notificação do usuário. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable @NonNull Long id,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Cliente cliente = clienteRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        NotificacaoDashboard notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        Long clienteIdLogado = Objects.requireNonNull(cliente.getId(), "ID do cliente logado está nulo");
        Long clienteIdNotificacao = Objects.requireNonNull(notificacao.getCliente().getId(), "ID do dono da notificação está nulo");

        if (!clienteIdNotificacao.equals(clienteIdLogado)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        notificacaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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