package umc.exs.controller_web.unitary;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import umc.exs.controller.web.LivroStoryController;

import static org.junit.jupiter.api.Assertions.*;

class LivroStoryControllerUnitTest {

    private final LivroStoryController controller = new LivroStoryController();

    @Test
    void deveExibirPaginaHistoriaLivro() {
        Model model = new ExtendedModelMap();

        String view = controller.paginaHistoriaLivro("1234567890", model);

        assertEquals("produto/historia_livro", view);
        assertEquals("1234567890", model.asMap().get("isbn"));
        assertEquals(false, model.asMap().get("unificado"));
    }

    @Test
    void deveExibirPaginaHistoriaLivroUnificado() {
        Model model = new ExtendedModelMap();

        String view = controller.paginaHistoriaLivroUnificado("1234567890", model);

        assertEquals("produto/historia_livro", view);
        assertEquals("1234567890", model.asMap().get("isbn"));
        assertEquals(true, model.asMap().get("unificado"));
    }

    @Test
    void deveRetornarViewTeste() {
        assertEquals("historia_livro", controller.teste());
    }
}
