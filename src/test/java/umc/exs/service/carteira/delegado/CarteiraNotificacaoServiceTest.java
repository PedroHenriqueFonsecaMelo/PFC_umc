package umc.exs.service.carteira.delegado;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.notificacao.NotificacaoService;

@ExtendWith(MockitoExtension.class)
class CarteiraNotificacaoServiceTest {

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private CarteiraNotificacaoService service;

    @Test
    void notificarRecarga_deveNotificarSaldoEDashboard() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setSaldoTokens(10.0);
        c.setEmail("c@test.com");

        service.notificarRecarga(c, 5.0, "PIX");

        verify(notificacaoService).notificarSaldo(eq(c.getId()), eq(c.getSaldoTokens()), contains("Recarga"));
        verify(notificacaoService).criarNotificacaoDashboard(eq(c), contains("Recarga confirmada"),
                eq("/clientes/carteira"));
    }

    @Test
    void notificarDebito_deveNotificarSaldo() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setSaldoTokens(10.0);

        service.notificarDebito(c, 3.0, "desc");

        verify(notificacaoService).notificarSaldo(eq(c.getId()), eq(c.getSaldoTokens()), contains("Débito"));
    }

    @Test
    void notificarPixConfirmado_deveNotificarSaldoEEnfileirarDashboard() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setSaldoTokens(13.0);

        service.notificarPixConfirmado(c, 3.0);

        verify(notificacaoService).notificarSaldo(eq(c.getId()), eq(c.getSaldoTokens()), contains("PIX"));
        verify(notificacaoService).criarNotificacaoDashboard(eq(c), contains("Recarga confirmada"),
                eq("/clientes/carteira"));
    }
}
