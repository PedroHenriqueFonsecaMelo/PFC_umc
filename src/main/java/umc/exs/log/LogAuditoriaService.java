package umc.exs.log;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.model.daos.repository.LogAuditoriaRepository;
import umc.exs.model.entidades.foundation.LogAuditoria;

@Service
public class LogAuditoriaService {

    @Autowired
    private LogAuditoriaRepository repository;

    public void registrarLog(String acao, Long idUsuario, String emailUsuario, String detalhes) {
        try {
            LogAuditoria la = new LogAuditoria(idUsuario, emailUsuario, acao, detalhes, LocalDateTime.now());
            repository.save(la);
        } catch (Exception ignored) {
            // Best-effort: swallow to avoid breaking auth flows
        }
    }

    public List<LogAuditoria> buscarLogsDoCliente(Long clienteId) {
        return repository.findByIdUsuarioOrderByDataHoraDesc(clienteId);
    }
}
