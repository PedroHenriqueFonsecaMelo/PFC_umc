package umc.exs.controller_web.unitary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import umc.exs.controller.web.LivroViewController;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.service.cliente.ClienteService;

class LivroViewControllerUnitTest {

    private final ClienteService clienteService = mock(ClienteService.class);

    private final LivroViewController controller =
            new LivroViewController(clienteService);

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
        User user = new User("usuario", "senha", Collections.emptyList());

        RedirectAttributes ra = new RedirectAttributesModelMap();

        Cliente cliente = mock(Cliente.class);

        when(cliente.getEnderecos())
            .thenReturn(Set.of(mock(Endereco.class)));

        when(clienteService.buscarClientePorEmail("usuario"))
                .thenReturn(Optional.of(cliente));

        String view = controller.paginaCheckout(user, ra);

        assertEquals("produto/checkout", view);
    }

    @Test
    void deveRedirecionarParaPerfilQuandoNaoPossuirEndereco() {
        User user = new User("usuario", "senha", Collections.emptyList());

        RedirectAttributes ra = new RedirectAttributesModelMap();

        Cliente cliente = mock(Cliente.class);

            
        when(cliente.getEnderecos())
            .thenReturn(Set.of());

        when(clienteService.buscarClientePorEmail("usuario"))
                .thenReturn(Optional.of(cliente));

        String view = controller.paginaCheckout(user, ra);

        assertEquals("redirect:/clientes/meu-perfil", view);
    }

    @Test
    void deveRetornarViewPedidoConfirmado() {
        String view = controller.paginaPedidoConfirmado();

        assertEquals("produto/pedido_confirmado", view);
    }
}