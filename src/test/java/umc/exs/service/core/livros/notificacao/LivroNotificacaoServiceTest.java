package umc.exs.service.core.livros.notificacao;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.core.dashboard.ListaDesejosService;
import umc.exs.service.notificacao.NotificacaoService;

@ExtendWith(MockitoExtension.class)
class LivroNotificacaoServiceTest {

    @Mock
    ListaDesejosService listaDesejosService;

    @Mock
    NotificacaoService notificacaoService;

    @InjectMocks
    LivroNotificacaoService service;

    @Test
    void notificarWishlistSeDisponivel_quandoErroNoWishlist_naoPropaga() {
        doThrow(new RuntimeException("falha")).when(listaDesejosService).notificarClientesSeDisponivel(anyString(),
                anyString());
        service.notificarWishlistSeDisponivel("isbn", "titulo");
        verify(listaDesejosService).notificarClientesSeDisponivel("isbn", "titulo");
    }

    @Test
    void notificarAprovacaoDashboard_deveChamarNotificacao() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("a@test.com");
        service.notificarAprovacaoDashboard(cliente, "Livro X", 10.0);
        verify(notificacaoService).criarNotificacaoDashboard(eq(cliente), contains("Seu livro 'Livro X'"), anyString());
    }
}
