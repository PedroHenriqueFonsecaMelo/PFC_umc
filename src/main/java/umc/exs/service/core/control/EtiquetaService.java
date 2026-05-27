package umc.exs.service.core.control;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.negocios.PedidoRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class EtiquetaService {

    private final PedidoRepository pedidoRepository;

    private static final String LOJA_NOME     = "Bibliotroca";
    private static final String LOJA_RUA      = "Rua das Letras, 100 - Centro";
    private static final String LOJA_CIDADE   = "São Paulo - SP";
    private static final String LOJA_CEP      = "01310-100";
    private static final String LOJA_EMAIL    = "bibliotroca.noreply@gmail.com";

    private static final Color COR_HEADER     = new Color(30, 30, 30);
    private static final Color COR_BORDA      = new Color(60, 60, 60);
    private static final Color COR_FUNDO_CEP  = new Color(240, 240, 240);
    private static final Color COR_TEXTO      = new Color(20, 20, 20);
    private static final Color COR_MUTED      = new Color(100, 100, 100);

    public byte[] gerarEtiqueta(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

        var comprador = pedido.getComprador();
        Endereco end = comprador.getEnderecos().stream()
            .min(Comparator.comparing(Endereco::getId))
            .orElseThrow(() -> new IllegalStateException("Comprador sem endereço."));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Tamanho A6 (10x15cm) — padrão de etiqueta de envio
            Rectangle pageSize = new Rectangle(283f, 425f);
            Document doc = new Document(pageSize, 12, 12, 12, 12);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();
            PdfContentByte canvas = writer.getDirectContent();

            // ── Fontes ──
            Font fBrand   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE);
            Font fHeader  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);
            Font fNome    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COR_TEXTO);
            Font fEnd     = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COR_TEXTO);
            Font fCep     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COR_TEXTO);
            Font fSmall   = FontFactory.getFont(FontFactory.HELVETICA, 7, COR_MUTED);
            Font fBold    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COR_TEXTO);
            Font fMeta    = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COR_MUTED);

            String dataEmissao = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            // ── TABELA PRINCIPAL ──
            PdfPTable main = new PdfPTable(1);
            main.setWidthPercentage(100);
            main.setSpacingAfter(0);

            // ════ CABEÇALHO DA LOJA ════
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidths(new float[]{3f, 1.5f});
            headerTable.setWidthPercentage(100);

            // Nome da loja (esquerda)
            PdfPCell brandCell = new PdfPCell();
            brandCell.setBackgroundColor(COR_HEADER);
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setPaddingLeft(10);
            brandCell.setPaddingTop(8);
            brandCell.setPaddingBottom(8);
            Paragraph brandPara = new Paragraph();
            brandPara.add(new Chunk("📚 " + LOJA_NOME + "\n", fBrand));
            brandPara.add(new Chunk("ETIQUETA DE ENVIO", fSmall));
            brandCell.addElement(brandPara);
            headerTable.addCell(brandCell);

            // Data emissão (direita)
            PdfPCell dataCell = new PdfPCell();
            dataCell.setBackgroundColor(COR_HEADER);
            dataCell.setBorder(Rectangle.NO_BORDER);
            dataCell.setPaddingRight(10);
            dataCell.setPaddingTop(8);
            dataCell.setPaddingBottom(8);
            dataCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph dataPara = new Paragraph();
            dataPara.setAlignment(Element.ALIGN_RIGHT);
            dataPara.add(new Chunk("Emitido em\n", fSmall));
            dataPara.add(new Chunk(dataEmissao, fSmall));
            dataCell.addElement(dataPara);
            headerTable.addCell(dataCell);

            PdfPCell headerWrap = new PdfPCell(headerTable);
            headerWrap.setPadding(0);
            headerWrap.setBorder(Rectangle.NO_BORDER);
            main.addCell(headerWrap);

            // ════ DESTINATÁRIO ════
            PdfPCell destHeader = new PdfPCell(new Phrase("▼  DESTINATÁRIO", fHeader));
            destHeader.setBackgroundColor(new Color(50, 50, 50));
            destHeader.setPaddingLeft(10);
            destHeader.setPaddingTop(5);
            destHeader.setPaddingBottom(5);
            destHeader.setBorder(Rectangle.NO_BORDER);
            main.addCell(destHeader);

            PdfPCell destBody = new PdfPCell();
            destBody.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
            destBody.setBorderColor(COR_BORDA);
            destBody.setBorderWidth(1.2f);
            destBody.setPaddingLeft(10);
            destBody.setPaddingRight(10);
            destBody.setPaddingTop(8);
            destBody.setPaddingBottom(8);

            Paragraph destPara = new Paragraph();
            destPara.setLeading(14f);
            destPara.add(new Chunk(comprador.getNome().toUpperCase() + "\n", fNome));
            destPara.add(new Chunk(
                nvl(end.getRua()) + ", " + nvl(end.getNumero()) +
                (end.getComplemento() != null && !end.getComplemento().isBlank()
                    ? " - " + end.getComplemento() : "") + "\n", fEnd));
            destPara.add(new Chunk("Bairro: " + nvl(end.getBairro()) + "\n", fEnd));
            destPara.add(new Chunk(nvl(end.getCidade()) + " - " + nvl(end.getEstado()) + "\n", fEnd));
            destBody.addElement(destPara);

            // CEP em destaque
            PdfPTable cepTable = new PdfPTable(1);
            cepTable.setWidthPercentage(100);
            PdfPCell cepCell = new PdfPCell(new Phrase("CEP: " + formatarCep(end.getCep()), fCep));
            cepCell.setBackgroundColor(COR_FUNDO_CEP);
            cepCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cepCell.setPaddingTop(4);
            cepCell.setPaddingBottom(4);
            cepCell.setBorder(Rectangle.NO_BORDER);
            cepTable.addCell(cepCell);
            destBody.addElement(cepTable);

            main.addCell(destBody);

            // ════ SEPARADOR ════
            PdfPCell sep = new PdfPCell(new Phrase(" "));
            sep.setFixedHeight(4f);
            sep.setBorder(Rectangle.NO_BORDER);
            main.addCell(sep);

            // ════ REMETENTE ════
            PdfPCell remHeader = new PdfPCell(new Phrase("▲  REMETENTE", fHeader));
            remHeader.setBackgroundColor(new Color(80, 80, 80));
            remHeader.setPaddingLeft(10);
            remHeader.setPaddingTop(4);
            remHeader.setPaddingBottom(4);
            remHeader.setBorder(Rectangle.NO_BORDER);
            main.addCell(remHeader);

            PdfPCell remBody = new PdfPCell();
            remBody.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
            remBody.setBorderColor(COR_BORDA);
            remBody.setBorderWidth(1.2f);
            remBody.setPaddingLeft(10);
            remBody.setPaddingRight(10);
            remBody.setPaddingTop(6);
            remBody.setPaddingBottom(6);

            Paragraph remPara = new Paragraph();
            remPara.setLeading(13f);
            remPara.add(new Chunk(LOJA_NOME + "\n", fBold));
            remPara.add(new Chunk(LOJA_RUA + "\n", fEnd));
            remPara.add(new Chunk(LOJA_CIDADE + "   CEP: " + LOJA_CEP + "\n", fEnd));
            remPara.add(new Chunk(LOJA_EMAIL, fSmall));
            remBody.addElement(remPara);
            main.addCell(remBody);

            // ════ SEPARADOR PONTILHADO ════
            PdfPCell sepPont = new PdfPCell(new Phrase("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
                FontFactory.getFont(FontFactory.HELVETICA, 6, COR_MUTED)));
            sepPont.setBorder(Rectangle.NO_BORDER);
            sepPont.setPaddingTop(4);
            sepPont.setPaddingBottom(4);
            sepPont.setHorizontalAlignment(Element.ALIGN_CENTER);
            main.addCell(sepPont);

            // ════ DADOS DO PEDIDO ════
            PdfPTable pedidoTable = new PdfPTable(2);
            pedidoTable.setWidths(new float[]{1f, 1f});
            pedidoTable.setWidthPercentage(100);

            // Pedido Nº
            PdfPCell pedidoNumCell = new PdfPCell();
            pedidoNumCell.setBorder(Rectangle.BOX);
            pedidoNumCell.setBorderColor(new Color(200, 200, 200));
            pedidoNumCell.setPadding(6);
            Paragraph pedidoNumPara = new Paragraph();
            pedidoNumPara.add(new Chunk("Pedido Nº\n", fSmall));
            pedidoNumPara.add(new Chunk("#" + String.format("%05d", pedido.getId()), fBold));
            pedidoNumCell.addElement(pedidoNumPara);
            pedidoTable.addCell(pedidoNumCell);

            // Rastreio
            // TODO: integrar com API dos Correios para código real
            String rastreio = pedido.getCodigoRastreio() != null
                ? pedido.getCodigoRastreio() : "—";
            PdfPCell rastreioCell = new PdfPCell();
            rastreioCell.setBorder(Rectangle.BOX);
            rastreioCell.setBorderColor(new Color(200, 200, 200));
            rastreioCell.setPadding(6);
            Paragraph rastreioPara = new Paragraph();
            rastreioPara.add(new Chunk("Código de Rastreio\n", fSmall));
            rastreioPara.add(new Chunk(rastreio, fBold));
            rastreioCell.addElement(rastreioPara);
            pedidoTable.addCell(rastreioCell);

            PdfPCell pedidoWrap = new PdfPCell(pedidoTable);
            pedidoWrap.setPadding(0);
            pedidoWrap.setBorder(Rectangle.NO_BORDER);
            main.addCell(pedidoWrap);

            // Livro
            PdfPCell livroCell = new PdfPCell();
            livroCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
            livroCell.setBorderColor(new Color(200, 200, 200));
            livroCell.setPaddingLeft(8);
            livroCell.setPaddingTop(4);
            livroCell.setPaddingBottom(6);
            Paragraph livroPara = new Paragraph();
            livroPara.add(new Chunk("Conteúdo: ", fSmall));
            livroPara.add(new Chunk(pedido.getTituloLivro(), fMeta));
            livroCell.addElement(livroPara);
            main.addCell(livroCell);

            doc.add(main);
            doc.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar etiqueta: " + e.getMessage(), e);
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String formatarCep(String cep) {
        if (cep == null) return "";
        String digits = cep.replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            return digits.substring(0, 5) + "-" + digits.substring(5);
        }
        return cep;
    }
}
