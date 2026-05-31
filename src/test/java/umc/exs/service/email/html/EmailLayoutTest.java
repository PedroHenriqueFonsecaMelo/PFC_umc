package umc.exs.service.email.html;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailLayoutTest {

    @Test
    void wrap_deveIncluirTituloEConteudo() {
        String html = EmailLayout.wrap("Meu Título", "<p>conteúdo</p>");
        assertTrue(html.contains("Meu Título"));
        assertTrue(html.contains("<p>conteúdo</p>"));
        assertTrue(html.startsWith("<!DOCTYPE html>"));
    }
}
