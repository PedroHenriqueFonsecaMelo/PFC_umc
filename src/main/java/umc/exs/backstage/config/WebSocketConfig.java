package umc.exs.backstage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Habilita o processamento de mensagens STOMP sobre WebSockets
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Define o prefixo para tópicos de destino (onde a aplicação envia mensagens)
        // Por exemplo, /topic/auditoria para enviar logs em tempo real
        config.enableSimpleBroker("/topic", "/queue"); 
        
        // Define o prefixo de destino para os Controllers (onde o cliente envia mensagens)
        // Por exemplo, o cliente envia para /app/log
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registra o endpoint que os clientes usarão para se conectar ao WebSocket.
        // O withSockJS permite fallback para navegadores que não suportam WebSocket nativamente.
        registry.addEndpoint("/ws-auditoria").withSockJS(); 
        
        // O Spring usa o SimpMessagingTemplate implicitamente uma vez que essa configuração é executada.
    }
}