package umc.exs.controller_web.unitary;

import org.junit.jupiter.api.Test;
import umc.exs.controller.web.LivroViewController;

import static org.junit.jupiter.api.Assertions.*;

class LivroViewControllerUnitTest {

    private final LivroViewController controller = new LivroViewController();

    @Test
    void deveRetornarViewVendaLivro() {
        assertEquals("produto/venda_livro", controller.paginaVender());
    }

    @Test
    void deveRetornarViewVitrineLivros() {
        assertEquals("produto/vitrine_livros", controller.paginaVitrine());
    }
}
