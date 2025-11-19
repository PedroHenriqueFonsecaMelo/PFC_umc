package umc.exs.backstage.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import umc.exs.model.daos.repository.LogAuditoriaRepository;
import umc.exs.model.dtos.auth.LogDTO;
import umc.exs.model.entidades.foundation.LogAuditoria;

@Service
public class LogAuditoriaService {
    private static final Logger logger = LoggerFactory.getLogger(LogAuditoriaService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    // Injeção do template de mensageria (para WebSockets)
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // NOVO: Injeção do repositório para persistência no banco de dados
    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    /**
     * Registra o log no arquivo/console, SALVA NO BANCO DE DADOS e envia para os
     * canais de notificação WebSocket.
     * * @param acao      Ação realizada (ex: "LOGIN_SUCESSO")
     * @param idUsuario ID do cliente envolvido (o alvo da ação)
     * @param emailUsuario Email do cliente (para detalhes no log)
     * @param detalhes  Mensagem detalhada
     */
    public void registrarLog(String acao, Long idUsuario, String emailUsuario, String detalhes) {
        
        LocalDateTime now = LocalDateTime.now();
        String timestampFormatado = now.format(FORMATTER);

        // Mensagem completa para registro em arquivo/console
        String logMessage = String.format(
                "[%s] [%s] Usuário %d (Email: %s): %s",
                timestampFormatado, acao, idUsuario, emailUsuario, detalhes);

        // 1. Grava no arquivo/console
        logger.info(logMessage);

        // 2. SALVA NO BANCO DE DADOS (NOVA FUNCIONALIDADE)
        try {
            LogAuditoria logEntidade = new LogAuditoria(
                idUsuario, 
                emailUsuario, 
                acao, 
                detalhes, 
                now
            );
            logAuditoriaRepository.save(logEntidade);
        } catch (Exception e) {
            // É crucial logar a falha do banco, mas não impedir o resto do log (WebSockets)
            logger.error("Falha ao salvar log de auditoria no banco de dados para a ação '{}'.", acao, e);
        }

        // 3. Cria o objeto DTO para transmissão WebSocket (usando timestamp formatado)
        LogDTO logDTO = new LogDTO(acao, idUsuario, detalhes, timestampFormatado);

        // 4. Notificação para o Administrador (Dashboard de Logs)
        // Admin subscreve o tópico /topic/admin/logs
        messagingTemplate.convertAndSend("/topic/admin/logs", logDTO);

        // 5. Notificação Específica para o Cliente (Pop-up/Alerta)
        if (acao.equals("LOGIN_SUCESSO") || acao.contains("FALHA") || acao.contains("SENHA")) {
            // Cliente subscreve um destino privado. ID do usuário é usado para roteamento.
            messagingTemplate.convertAndSendToUser(
                    idUsuario.toString(),
                    "/queue/notificacoes",
                    logDTO);
        }
    }
    public List<LogAuditoria> buscarLogsDoCliente(Long idUsuario){
        return logAuditoriaRepository.findByIdUsuarioOrderByDataHoraDesc(idUsuario);
    }
}