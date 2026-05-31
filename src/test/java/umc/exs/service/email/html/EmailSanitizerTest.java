package umc.exs.service.email.html;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailSanitizerTest {

    @Test
    void esc_deveEscaparCaracteresHTML() {
        String original = "<tag> & \" '";
        String escaped = EmailSanitizer.esc(original);
        assertTrue(escaped.contains("&lt;tag&gt;"));
        assertTrue(escaped.contains("&amp;"));
        assertTrue(escaped.contains("&quot;"));
        assertTrue(escaped.contains("&#39;"));
    }

    @Test
    void esc_quandoNulo_retornaVazio() {
        assertEquals("", EmailSanitizer.esc(null));
    }
}
