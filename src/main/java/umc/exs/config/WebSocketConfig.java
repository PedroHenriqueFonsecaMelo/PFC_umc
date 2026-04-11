package umc.exs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.NonNull;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura broker mensagens simples STOMP WebSocket.
     * Habilita /topic /queue, prefixo /app.
     * 
     * @param config registry broker
     */
    @Override
    public void configureMessageBroker(@SuppressWarnings("null") @NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra endpoint STOMP /ws-auditoria com SockJS fallback.
     * Suporte navegadores WebSocket antigos.
     * 
     * @param registry endpoints STOMP
     */
    @SuppressWarnings("null")
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-auditoria").withSockJS();
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Configuração WebSocket STOMP para auditoria real-time.
 * Broker /topic/queue, endpoint /ws-auditoria SockJS.
 * Enable @EnableWebSocketMessageBroker.
 */
