package umc.exs.service.email.html;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailComponentsTest {

    @Test
    void button_deveGerarLink() {
        String html = EmailComponents.button("Clique", "https://x");
        assertTrue(html.contains("<a href='https://x'"));
        assertTrue(html.contains("Clique"));
    }
}
