package umc.exs.service.log;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.foundation.LogAuditoria;
import umc.exs.repository.LogAuditoriaRepository;

@Service
public class LogAuditoriaService {

    @Autowired
    private LogAuditoriaRepository repository;

/**
 * Registra log ação usuário no banco.
 * Cria LogAuditoria, salva repository (best-effort).
 * Não quebra fluxos se erro.
 * @param acao tipo ação
 * @param idUsuario cliente ID
 * @param emailUsuario email
 * @param detalhes descrição
 */
    public void registrarLog(String acao, Long idUsuario, String emailUsuario, String detalhes) {

        try {
            LogAuditoria la = new LogAuditoria(idUsuario, emailUsuario, acao, detalhes, LocalDateTime.now());
            repository.save(la);
        } catch (Exception ignored) {
            // Best-effort: swallow to avoid breaking auth flows
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
 * @param clienteId ID cliente
 * @return List<LogAuditoria> recente primeiro
 */
    public List<LogAuditoria> buscarLogsDoCliente(Long clienteId) {

        return repository.findByIdUsuarioOrderByDataHoraDesc(clienteId);
    }
}
