package umc.exs.service.log;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.repository.logic.LogAuditoriaRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAuditoriaService {

    private final LogAuditoriaRepository repository;

    /**
     * Registra log ação usuário no banco.
     * Cria LogAuditoria, salva repository (best-effort).
     * Não quebra fluxos se erro.
     * 
     * @param acao         tipo ação
     * @param idUsuario    cliente ID
     * @param emailUsuario email
     * @param detalhes     descrição
     */
    public void registrarLog(String acao, Long idUsuario, String emailUsuario, String detalhes) {

        try {
            LogAuditoria la = new LogAuditoria(idUsuario, emailUsuario, acao, detalhes, LocalDateTime.now());
            repository.save(la);
        } catch (Exception e) {
            // Best-effort: não quebra o fluxo principal, mas registra no console
            log.warn("Falha ao salvar log de auditoria [acao={}, usuarioId={}]: {}", acao, idUsuario, e.getMessage());
        }
    }

    /**
     * DESCRIÇÃO DO ARQUIVO:
     * Serviço auditoria registra/logs ações usuário banco.
     * registrarLog salva acao/id/email/detalhes (best-effort).
     * buscarLogsDoCliente lista ordenada data desc cliente.
     * Usado controllers/services para rastreio.
     */

    /**
     * Busca logs cliente ordenados data desc.
     * Usa repository findByIdUsuarioOrderByDataHoraDesc.
     * 
     * @param clienteId ID cliente
     * @return List<LogAuditoria> recente primeiro
     */
    public List<LogAuditoria> buscarLogsDoCliente(Long clienteId) {

        return repository.findByIdUsuarioOrderByDataHoraDesc(clienteId);
    }

    public List<LogAuditoria> buscarTodosLogs() {

        return repository.findAllByOrderByDataHoraDesc();
    }
}
