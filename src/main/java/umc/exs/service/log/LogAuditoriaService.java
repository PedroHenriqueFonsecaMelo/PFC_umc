package umc.exs.service.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.logic.LogAuditoriaRepository;
import umc.exs.repository.usuario.ClienteRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAuditoriaService {

    private final LogAuditoriaRepository repository;
    private final ClienteRepository clienteRepository;

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Registra log ação usuário no banco.
     */
    public void registrarLog(String acao, Long idUsuario, String emailUsuario, String detalhes) {

        try {
            LocalDateTime agora = LocalDateTime.now();

            LogAuditoria la = new LogAuditoria(
                    idUsuario,
                    emailUsuario,
                    acao,
                    detalhes,
                    agora);

            String dataFormatada = FORMATTER.format(agora);

            auditLog.info("ACAO={} USUARIO={} EMAIL={} DETALHES={} DATA_HORA={}",
                    acao, idUsuario, emailUsuario, detalhes, dataFormatada);

            repository.save(la);

        } catch (Exception e) {
            log.warn("Falha ao salvar log de auditoria [acao={}, usuarioId={}]: {}",
                    acao, idUsuario, e.getMessage());
        }
    }

    public void registrarLog(String acao, String emailUsuario, String detalhes) {

        try {
            LocalDateTime agora = LocalDateTime.now();

            LogAuditoria la = new LogAuditoria(
                    emailUsuario,
                    acao,
                    detalhes,
                    agora);

            String dataFormatada = FORMATTER.format(agora);

            auditLog.info("ACAO={} EMAIL={} DETALHES={} DATA_HORA={}",
                    acao, emailUsuario, detalhes, dataFormatada);

            repository.save(la);

        } catch (Exception e) {
            log.warn("Falha ao salvar log de auditoria [acao={}]: {}",
                    acao, e.getMessage());
        }
    }
    public void registrarLog(String acao, String detalhes) {

        try {
            LocalDateTime agora = LocalDateTime.now();

            LogAuditoria la = new LogAuditoria(
                    acao,
                    detalhes,
                    agora);

            String dataFormatada = FORMATTER.format(agora);

            auditLog.info("ACAO={} DETALHES={} DATA_HORA={}",
                    acao, detalhes, dataFormatada);

            repository.save(la);

        } catch (Exception e) {
            log.warn("Falha ao salvar log de auditoria [acao={}]: {}",
                    acao, e.getMessage());
        }
    }

    /**
     * Busca logs a partir da criação do usuário.
     */
    public List<LogAuditoria> buscarLogsDoCliente(Long clienteId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return repository.findByIdUsuarioAndDataHoraAfterOrderByDataHoraDesc(
                clienteId,
                cliente.getDataCriacao());
    }

    public List<LogAuditoria> buscarTodosLogs() {
        return repository.findAllByOrderByDataHoraDesc();
    }

    public List<LogAuditoria> buscarComFiltros(
            String emailUsuario,
            String acao,
            String dataInicio,
            String dataFim) {

        // Ajusta filtros de string
        String email = (emailUsuario == null || emailUsuario.isBlank()) ? null : emailUsuario.trim();
        String acaoFiltro = (acao == null || acao.isBlank()) ? null : acao.trim();

        // Formato esperado para LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Converte strings em LocalDateTime, ou null se não informado
        LocalDateTime inicioLDT = null;
        if (dataInicio != null && !dataInicio.isBlank()) {
            LocalDate date = LocalDate.parse(dataInicio);
            inicioLDT = date.atStartOfDay();
        }

        LocalDateTime fimLDT = null;
        if (dataFim != null && !dataFim.isBlank()) {
            fimLDT = LocalDateTime.parse(dataFim + " 23:59", formatter);
        }

        // Chama o repositório com LocalDateTime
        return repository.buscarComFiltros(email, acaoFiltro, inicioLDT, fimLDT);
    }

    public List<String> buscarAcoesDistintas() {
        return repository.findAcoesDistintas();
    }

    /**
     * CSV export corrigido (LocalDateTime seguro)
     */
    public String exportarCSV(List<LogAuditoria> logs) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("ID,Acao,Email Usuario,ID Usuario,Detalhes,Data Hora");

        for (LogAuditoria l : logs) {
            pw.printf("%d,\"%s\",\"%s\",%s,\"%s\",\"%s\"%n",
                    l.getId(),
                    escape(l.getAcao()),
                    escape(l.getEmailUsuario()),
                    l.getIdUsuario() != null ? l.getIdUsuario() : "",
                    escape(l.getDetalhes()),
                    format(l.getDataHora()));
        }

        return sw.toString();
    }

    /**
     * PDF export corrigido (LocalDateTime seguro)
     */
    public byte[] exportarPDF(List<LogAuditoria> logs) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 40, 30);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph titulo = new Paragraph("Relatório de Auditoria — Bibliotroca", titleFont);
            titulo.setAlignment(Element.ALIGN_LEFT);
            titulo.setSpacingAfter(6);
            doc.add(titulo);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Paragraph sub = new Paragraph(
                    "Gerado em: " + format(LocalDateTime.now())
                            + " | Total: " + logs.size(),
                    subFont);
            sub.setSpacingAfter(12);
            doc.add(sub);

            float[] widths = { 3f, 14f, 18f, 6f, 35f, 14f };
            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);

            Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            hFont.setColor(255, 255, 255);

            java.awt.Color hBg = new java.awt.Color(74, 93, 35);

            for (String col : new String[] { "#", "Ação", "E-mail", "ID", "Detalhes", "Data/Hora" }) {
                PdfPCell cell = new PdfPCell(new Phrase(col, hFont));
                cell.setBackgroundColor(hBg);
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font rFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
            java.awt.Color altBg = new java.awt.Color(249, 246, 240);

            for (int i = 0; i < logs.size(); i++) {

                LogAuditoria l = logs.get(i);

                java.awt.Color bg = (i % 2 == 1) ? altBg : java.awt.Color.WHITE;

                addCell(table, String.valueOf(i + 1), rFont, bg);
                addCell(table, l.getAcao(), rFont, bg);
                addCell(table, l.getEmailUsuario(), rFont, bg);
                addCell(table, String.valueOf(l.getIdUsuario()), rFont, bg);
                addCell(table, truncar(l.getDetalhes(), 120), rFont, bg);
                addCell(table, format(l.getDataHora()), rFont, bg);
            }

            doc.add(table);
            doc.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar PDF de auditoria", e);
        }
    }

    // ================= helpers =================

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\"", "\"\"");
    }

    private String format(LocalDateTime dt) {
        if (dt == null)
            return "—";
        return FORMATTER.format(dt);
    }

    private String truncar(String s, int max) {
        if (s == null)
            return "—";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private void addCell(PdfPTable table, String text, Font font, java.awt.Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        table.addCell(cell);
    }
}