package umc.exs.service.email.html;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailComponentsTest {

    @Test
    void h1_deveGerarTagH1() {
        assertEquals("<h1 style='margin:0 0 10px 0;'>Teste</h1>", EmailComponents.h1("Teste"));
    }

    @Test
    void button_deveGerarLink() {
        String html = EmailComponents.button("Clique", "https://x");
        assertTrue(html.contains("<a href='https://x'"));
        assertTrue(html.contains("Clique"));
    }
}
