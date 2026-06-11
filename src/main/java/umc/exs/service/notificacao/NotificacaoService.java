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
 * e notificações de dashboard persistidas no banco de dados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacaoDashboardRepository notificacaoDashboardRepository;

    /**
     * Envia uma notificação de atualização de saldo ao cliente em tempo real via WebSocket.
     * Publica no tópico /topic/saldo/{clienteId} para atualização instantânea na interface.
     */
    public void notificarSaldo(Long clienteId, Double novoSaldo, String descricao) {
        try {
            Map<String, Object> payload = Map.of(
                    "clienteId", clienteId,
                    "novoSaldo", novoSaldo,
                    "descricao", descricao != null ? descricao : "",
                    "timestamp", LocalDateTime.now().toString());

            // Valida os campos obrigatórios antes de publicar no WebSocket
            if (payload.get("clienteId") == null || payload.get("novoSaldo") == null) {
                log.error("Dados insuficientes para notificação de saldo: clienteId ou novoSaldo nulos.");
                return;
            }
            messagingTemplate.convertAndSend("/topic/saldo/" + clienteId, payload);
            log.debug("Notificação de saldo enviada para cliente ID {}: T$ {}", clienteId, novoSaldo);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação WebSocket para cliente {}: {}", clienteId, e.getMessage());
        }
    }

    /**
     * Cria e persiste uma notificação no dashboard do cliente com mensagem e link de redirecionamento.
     * Retorna null silenciosamente em caso de falha para não interromper o fluxo principal.
     */
    @Transactional
    public NotificacaoDashboard criarNotificacaoDashboard(Cliente cliente, String mensagem, String link) {
        NotificacaoDashboard notificacao = NotificacaoDashboard.builder()
                .cliente(cliente)
                .mensagem(mensagem)
                .dataCriacao(LocalDateTime.now())
                .link(link)
                .build();

        // Garante que os campos mínimos estão preenchidos antes de persistir
        if (notificacao.getCliente() == null || notificacao.getMensagem() == null) {
            log.error("Dados insuficientes para criar notificação de dashboard: cliente ou mensagem nulos.");
            return null;
        }

        try {
            return notificacaoDashboardRepository.save(notificacao);
        } catch (Exception e) {
            log.error("Falha ao salvar notificação de dashboard para cliente {}: {}", cliente.getId(), e.getMessage());
            return null;
        }
    }
}
