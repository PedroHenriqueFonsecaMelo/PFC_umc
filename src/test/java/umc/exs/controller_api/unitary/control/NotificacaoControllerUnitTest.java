package umc.exs.controller_api.unitary.control;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import umc.exs.controller.api.control.NotificacaoController;
import umc.exs.model.entidades.foundation.NotificacaoDashboard;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.NotificacaoDashboardRepository;
import umc.exs.repository.usuario.ClienteRepository;

class NotificacaoControllerUnitTest {

    private NotificacaoDashboardRepository notificacaoRepository;
    private ClienteRepository clienteRepository;
    private NotificacaoController controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        notificacaoRepository = mock(NotificacaoDashboardRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        controller = new NotificacaoController(notificacaoRepository, clienteRepository);

        user = User.withUsername("cliente@email.com")
                .password("pass")
                .authorities("USER")
                .build();
    }

    @Test
    void listar_SemAuth_Retorna401() {
        ResponseEntity<List<Map<String, Object>>> resp = controller.listar(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(clienteRepository, notificacaoRepository);
    }

    @Test
    void marcarLida_DeOutraPessoa_Retorna403() {
        Cliente clienteLogado = mock(Cliente.class);
        when(clienteLogado.getId()).thenReturn(1L);

        when(clienteRepository.findByEmail(eq(user.getUsername())))
                .thenReturn(Optional.of(clienteLogado));

        NotificacaoDashboard notif = mock(NotificacaoDashboard.class);
        Cliente donoNotif = mock(Cliente.class);
        when(donoNotif.getId()).thenReturn(2L);
        when(notif.getCliente()).thenReturn(donoNotif);

        when(notificacaoRepository.findById(eq(10L)))
                .thenReturn(Optional.of(notif));

        ResponseEntity<Map<String, String>> resp = controller.marcarLida(10L, user);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("Acesso negado.", resp.getBody().get("erro"));
    }

    @Test
    void criar_SemMensagem_Retorna400() {
        ResponseEntity<Map<String, Object>> resp = controller.criar(Map.of("mensagem", "  "), user);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Mensagem obrigatória.", resp.getBody().get("erro"));
        verifyNoInteractions(notificacaoRepository);
    }

    @Test
    void criar_ComMensagem_ParaUsuario_Retorna201() {
        Cliente clienteLogado = mock(Cliente.class);
        when(clienteLogado.getId()).thenReturn(1L);
        when(clienteLogado.getEmail()).thenReturn(user.getUsername());
        when(clienteLogado.getNome()).thenReturn("Cliente");

        when(clienteRepository.findByEmail(eq(user.getUsername())))
                .thenReturn(Optional.of(clienteLogado));

        NotificacaoDashboard salvo = mock(NotificacaoDashboard.class);
        when(salvo.getId()).thenReturn(5L);
        when(salvo.getMensagem()).thenReturn("Oi");
        when(salvo.getDataCriacao()).thenReturn(LocalDateTime.now());
        when(salvo.isLida()).thenReturn(false);
        when(salvo.getLink()).thenReturn(null);
        when(notificacaoRepository.save(any(NotificacaoDashboard.class))).thenReturn(salvo);

        ResponseEntity<Map<String, Object>> resp = controller.criar(Map.of("mensagem", "Oi"), user);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        verify(notificacaoRepository).save(any(NotificacaoDashboard.class));
    }
}

