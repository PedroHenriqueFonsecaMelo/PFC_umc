package umc.exs.service.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

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

    /** Gera PDF com os logs fornecidos e retorna os bytes. */
    public byte[] exportarPDF(List<LogAuditoria> logs) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 40, 30);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Título ──
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph titulo = new Paragraph("Relatório de Auditoria — Bibliotroca", titleFont);
            titulo.setAlignment(Element.ALIGN_LEFT);
            titulo.setSpacingAfter(4);
            doc.add(titulo);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Paragraph sub = new Paragraph(
                "Gerado em: " + java.time.format.DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now())
                    + "   |   Total: " + logs.size() + " registros", subFont);
            sub.setSpacingAfter(12);
            doc.add(sub);

            // ── Tabela ──
            float[] widths = {3f, 14f, 18f, 6f, 35f, 14f};
            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);

            // Cabeçalho
            Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            hFont.setColor(255, 255, 255);
            java.awt.Color hBg = new java.awt.Color(74, 93, 35);
            for (String col : new String[]{"#", "Ação", "E-mail", "ID", "Detalhes", "Data/Hora"}) {
                PdfPCell cell = new PdfPCell(new Phrase(col, hFont));
                cell.setBackgroundColor(hBg);
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            // Linhas
            Font rFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
            java.awt.Color altBg = new java.awt.Color(249, 246, 240);
            for (int i = 0; i < logs.size(); i++) {
                LogAuditoria l = logs.get(i);
                java.awt.Color rowBg = (i % 2 == 1) ? altBg : java.awt.Color.WHITE;
                String[] vals = {
                    String.valueOf(i + 1),
                    safe(l.getAcao()),
                    safe(l.getEmailUsuario()),
                    l.getIdUsuario() != null ? l.getIdUsuario().toString() : "—",
                    truncar(safe(l.getDetalhes()), 120),
                    safe(l.getDataHora())
                };
                for (String val : vals) {
                    PdfPCell cell = new PdfPCell(new Phrase(val, rFont));
                    cell.setBackgroundColor(rowBg);
                    cell.setPadding(4);
                    table.addCell(cell);
                }
            }

            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF de auditoria", e);
        }
    }

    private String safe(String s) {
        return s != null ? s : "—";
    }

    private String truncar(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
