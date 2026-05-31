package umc.exs.controller_web.unitary;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import umc.exs.controller.web.LivroViewController;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

class LivroViewControllerUnitTest {

    private final LivroViewController controller = new LivroViewController();

    @Test
    void deveRetornarViewVendaLivro_comClienteLogado() {
        User user = new User("usuario", "senha", Collections.emptyList());
        Model model = new ExtendedModelMap();

        String view = controller.paginaVender(user, model);

        assertEquals("produto/venda_livro", view);
        assertTrue((Boolean) model.getAttribute("clienteLogado"));
    }

    @Test
    void deveRetornarViewVendaLivro_semClienteLogado() {
        Model model = new ExtendedModelMap();

        String view = controller.paginaVender(null, model);

        assertEquals("produto/venda_livro", view);
        assertFalse((Boolean) model.getAttribute("clienteLogado"));
    }

    @Test
    void deveRetornarViewVitrineLivros() {
        String view = controller.paginaVitrine();
        assertEquals("produto/vitrine_livros", view);
    }

    @Test
    void deveRetornarViewLivroDetalhe() {
        String view = controller.paginaLivro(42L);
        assertEquals("produto/livro_detalhe", view);
    }

    @Test
    void deveRetornarViewEstante() {
        String view = controller.paginaEstante();
        assertEquals("produto/estante", view);
    }

    @Test
    void deveRetornarViewCheckout() {
        String view = controller.paginaCheckout();
        assertEquals("produto/checkout", view);
    }

    @Test
    void deveRetornarViewPedidoConfirmado() {
        String view = controller.paginaPedidoConfirmado();
        assertEquals("produto/pedido_confirmado", view);
    }
}