package umc.exs.service.notificacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import umc.exs.model.entidades.foundation.NotificacaoDashboard;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.NotificacaoDashboardRepository;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    NotificacaoDashboardRepository notificacaoDashboardRepository;

    @InjectMocks
    NotificacaoService service;

    @Test
    void notificarSaldo_deveEnviarMensagemQuandoValido() {
        service.notificarSaldo(1L, 10.0, "Saldo atualizado");
        verify(messagingTemplate).convertAndSend(eq("/topic/saldo/1"), any(Map.class));
    }

    @Test
    void notificarSaldo_quandoDadosInvalidos_naoEnvia() {
        service.notificarSaldo(1L, null, "Saldo atualizado");
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void criarNotificacaoDashboard_quandoDadosValidados_salvaEntidade() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Teste");
        cliente.setEmail("test@test.com");

        NotificacaoDashboard saved = new NotificacaoDashboard();
        when(notificacaoDashboardRepository.save(any())).thenReturn(saved);

        NotificacaoDashboard result = service.criarNotificacaoDashboard(cliente, "Mensagem", "/link");

        assertSame(saved, result);
    }

    @Test
    void criarNotificacaoDashboard_quandoMensagemNula_retornaNull() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Teste");
        cliente.setEmail("test@test.com");

        NotificacaoDashboard result = service.criarNotificacaoDashboard(cliente, null, "/link");
        assertNull(result);
        verifyNoInteractions(notificacaoDashboardRepository);
    }

    @Test
    void notificarSaldo_quandoClienteIdNull_naoEnvia() {
        service.notificarSaldo(null, 10.0, "Saldo");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void notificarSaldo_quandoDescricaoNull_enviaComDescricaoVazia() {
        service.notificarSaldo(1L, 10.0, null);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/saldo/1"),
                argThat((Object payload) -> {
                    Map<?, ?> map = (Map<?, ?>) payload;
                    return map.get("descricao").equals("");
                })
        );
    }

    @Test
    void criarNotificacaoDashboard_quandoClienteNull_retornaNull() {
        NotificacaoDashboard result = service.criarNotificacaoDashboard(null, "Mensagem", "/link");

        assertNull(result);
        verifyNoInteractions(notificacaoDashboardRepository);
    }

    @Test
    void criarNotificacaoDashboard_quandoErroNoSave_retornaNull() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(notificacaoDashboardRepository.save(any()))
                .thenThrow(new RuntimeException());

        NotificacaoDashboard result = service.criarNotificacaoDashboard(cliente, "Mensagem", "/link");

        assertNull(result);
    }
}
