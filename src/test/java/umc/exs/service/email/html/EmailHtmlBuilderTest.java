package umc.exs.service.email.html;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class EmailHtmlBuilderTest {

    @Test
    void compraSucesso_deveGerarHtmlCompleto() {

        String html = EmailHtmlBuilder.compraSucesso(
                "Maria",
                "Livro Legal",
                39.90,
                150.00,
                "02/06/2026");

        assertAll(
                () -> assertTrue(html.contains("Compra realizada com sucesso")),
                () -> assertTrue(html.contains("Maria")),
                () -> assertTrue(html.contains("Livro Legal")),
                () -> assertTrue(html.contains("39.9")),
                () -> assertTrue(html.contains("<html")),
                () -> assertTrue(html.contains("</html>")),
                () -> assertTrue(html.contains("Bibliotroca")));
    }

    @Test
    void cancelamentoRecusado_deveGerarHtmlComMotivo() {
        String html = EmailHtmlBuilder.cancelamentoRecusado("João", 10L, "Livro X", "Não aprovado");
        assertTrue(html.contains("Cancelamento recusado"));
        assertTrue(html.contains("Não aprovado"));
    }

    @Test
    void carrinhoConfirmado_deveIncluirItens() {
        List<String[]> itens = List.<String[]>of(new String[] { "Livro A", "19.90" });
        String html = EmailHtmlBuilder.carrinhoConfirmado("Ana", itens, 19.90, 0.0, "01/01/2025");
        assertTrue(html.contains("Livro A"));
        assertTrue(html.contains("Total: R$ 19.9"));
    }

    @Test
    void atualizacaoSaldo_deveFormatarData() {
        String html = EmailHtmlBuilder.atualizacaoSaldo("Ana", 10.0, 0.0, 10.0, "Crédito", true, LocalDateTime.now());
        assertTrue(html.contains("Movimentação financeira"));
        assertTrue(html.contains("Operação: Crédito"));
    }
}
