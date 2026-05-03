package umc.exs.service.log;

import java.io.PrintWriter;
import java.io.StringWriter;
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

    public List<LogAuditoria> buscarComFiltros(String emailUsuario, String acao, String dataInicio, String dataFim) {
        String email = (emailUsuario == null || emailUsuario.isBlank()) ? null : emailUsuario.trim();
        String acaoFiltro = (acao == null || acao.isBlank()) ? null : acao.trim();
        String inicio = (dataInicio == null || dataInicio.isBlank()) ? null : dataInicio + " 00:00";
        String fim = (dataFim == null || dataFim.isBlank()) ? null : dataFim + " 23:59";
        return repository.buscarComFiltros(email, acaoFiltro, inicio, fim);
    }

    public List<String> buscarAcoesDistintas() {
        return repository.findAcoesDistintas();
    }

    public String exportarCSV(List<LogAuditoria> logs) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("ID,Acao,Email Usuario,ID Usuario,Detalhes,Data Hora");
        for (LogAuditoria l : logs) {
            pw.printf("%d,\"%s\",\"%s\",%s,\"%s\",\"%s\"%n",
                l.getId(),
                escape(l.getAcao()),
                escape(l.getEmailUsuario()),
                l.getIdUsuario() != null ? l.getIdUsuario().toString() : "",
                escape(l.getDetalhes()),
                escape(l.getDataHora())
            );
        }
        return sw.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
