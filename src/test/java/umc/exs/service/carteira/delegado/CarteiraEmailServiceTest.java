package umc.exs.service.carteira.delegado;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.log.AppLogger;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class CarteiraEmailServiceTest {

    @Mock
    private EmailFacade emailFacade;

    @InjectMocks
    private CarteiraEmailService service;

    @Mock
    AppLogger appLogger;

    @Mock
    LogAuditoriaService logAuditoriaService;

    @Test
    void enviarCredito_deveEnviarMotivoPIX() {
        Cliente c = new Cliente();
        c.setEmail("c@test.com");
        c.setNome("Nome");
        c.setSaldoTokens(15.0);

        service.enviarCredito(c, 10.0, 5.0, "PIX");

        verify(emailFacade).sendHtmlSafe(eq(c.getEmail()), anyString(), contains("PIX"));
    }

    @Test
    void enviarDebito_deveEnviar() {
        Cliente c = new Cliente();
        c.setEmail("c@test.com");
        c.setNome("Nome");
        c.setSaldoTokens(5.0);

        service.enviarDebito(c, 10.0, 5.0, "desc");
        verify(emailFacade).sendHtmlSafe(eq(c.getEmail()), anyString(), anyString());
    }

    @Test
    void enviarConfirmacaoPix_deveEnviar() {
        Cliente c = new Cliente();
        c.setEmail("c@test.com");
        c.setNome("Nome");
        c.setSaldoTokens(13.0);

        service.enviarConfirmacaoPix(c, 10.0, 3.0);
        verify(emailFacade).sendHtmlSafe(eq(c.getEmail()), anyString(), anyString());
    }
}
