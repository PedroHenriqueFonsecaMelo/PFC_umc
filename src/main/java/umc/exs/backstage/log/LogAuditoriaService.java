package umc.exs.backstage.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import umc.exs.model.dtos.auth.LogDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LogAuditoriaService {
private static final Logger logger = LoggerFactory.getLogger(LogAuditoriaService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    // Injeção do template de mensageria (para WebSockets)
    @Autowired
    private SimpMessagingTemplate messagingTemplate; 

    /**
     * Registra o log no arquivo/console e envia para os canais de notificação WebSocket.
     * @param acao Ação realizada (ex: "LOGIN_SUCESSO", "ADICIONAR_ENDERECO")
     * @param idUsuario ID do cliente envolvido (o alvo da ação)
     * @param emailUsuario Email do cliente (para detalhes no log)
     * @param detalhes Mensagem detalhada
     */
    public void registrarLog(String acao, Long idUsuario, String emailUsuario, String detalhes) {
        
        String timestamp = LocalDateTime.now().format(FORMATTER);
        
        // Mensagem completa para registro em arquivo/console
        String logMessage = String.format(
            "[%s] [%s] Usuário %d (Email: %s): %s",
            timestamp, acao, idUsuario, emailUsuario, detalhes
        );

        // 1. Grava no arquivo/console
        logger.info(logMessage);

        // 2. Cria o objeto DTO para transmissão WebSocket
        LogDTO logDTO = new LogDTO(acao, idUsuario, detalhes, timestamp);

        // 3. Notificação para o Administrador (Dashboard de Logs)
        // Admin subscreve o tópico /topic/admin/logs
        messagingTemplate.convertAndSend("/topic/admin/logs", logDTO);

        // 4. Notificação Específica para o Cliente (Pop-up/Alerta)
        if (acao.equals("LOGIN_SUCESSO") || acao.contains("FALHA") || acao.contains("SENHA")) {
            // Cliente subscreve um destino privado. ID do usuário é usado para roteamento.
            messagingTemplate.convertAndSendToUser(
                idUsuario.toString(),
                "/queue/notificacoes", 
                logDTO
            );
        }
    }
}