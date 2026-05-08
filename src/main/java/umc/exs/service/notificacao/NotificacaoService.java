package umc.exs.service.notificacao;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.NotificacaoDashboard;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.NotificacaoDashboardRepository;

import java.util.Map;

/**
 * Serviço central de notificações em tempo real (WebSocket/STOMP)
 * e notificações de dashboard persistidas no banco.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacaoDashboardRepository notificacaoDashboardRepository;

    /**
     * Notifica o cliente sobre mudança de saldo via WebSocket (/topic/saldo/{clienteId}).
     * Também cria registro de notificação no dashboard.
     */
    public void notificarSaldo(Long clienteId, Double novoSaldo, String descricao) {
        try {
            Map<String, Object> payload = Map.of(
                    "clienteId", clienteId,
                    "novoSaldo", novoSaldo,
                    "descricao", descricao != null ? descricao : "",
                    "timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/saldo/" + clienteId, payload);
            log.debug("Notificação de saldo enviada para cliente ID {}: T$ {}", clienteId, novoSaldo);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação WebSocket para cliente {}: {}", clienteId, e.getMessage());
        }
    }

    /**
     * Cria uma notificação persistida no dashboard do cliente.
     */
    @Transactional
    public NotificacaoDashboard criarNotificacaoDashboard(Cliente cliente, String mensagem, String link) {
        NotificacaoDashboard notificacao = NotificacaoDashboard.builder()
                .cliente(cliente)
                .mensagem(mensagem)
                .dataCriacao(LocalDateTime.now())
                .link(link)
                .build();
        return notificacaoDashboardRepository.save(notificacao);
    }
}
