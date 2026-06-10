package umc.exs.service.core.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.foundation.ListaDesejos;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.ListaDesejosRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.log.AppLogger;
import umc.exs.service.notificacao.NotificacaoService;

@ExtendWith(MockitoExtension.class)
class ListaDesejosServiceUnitTest {

    @Mock
    ListaDesejosRepository listaDesejosRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    EmailFacade emailFacade;

    @Mock
    NotificacaoService notificacaoService;

    @Mock
    AppLogger appLogger;

    @InjectMocks
    ListaDesejosService service;

    @BeforeEach
    void setUp() {
    }

    @Test
    void adicionarDesejo_quandoISBNJaExiste_deveLancarRuntimeException() {
        Cliente c = new Cliente();
        c.setId(1L);

        when(clienteRepository.findByEmail("email@test.com")).thenReturn(Optional.of(c));
        when(listaDesejosRepository.existsByClienteIdAndIsbn(eq(1L), eq("ISBN"))).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> service.adicionarDesejo("email@test.com", "ISBN"));

        verify(listaDesejosRepository, never()).save(any());
    }

    @Test
    void adicionarDesejo_quandoClienteNaoExiste_deveLancarRuntimeException() {
        when(clienteRepository.findByEmail("email@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.adicionarDesejo("email@test.com", "ISBN"));

        verifyNoInteractions(listaDesejosRepository);
    }

    @Test
    void togglePreReserva_quandoPertenceAoCliente_deveAlternarESalvar() {
        Cliente c = new Cliente();
        c.setId(1L);

        ListaDesejos desejo = new ListaDesejos();
        desejo.setId(10L);
        desejo.setCliente(c);
        desejo.setIsbn("ISBN");
        desejo.setPreReservaAtiva(false);

        when(clienteRepository.findByEmail("email@test.com")).thenReturn(Optional.of(c));
        when(listaDesejosRepository.findById(10L)).thenReturn(Optional.of(desejo));
        when(listaDesejosRepository.save(any(ListaDesejos.class))).thenAnswer(i -> i.getArgument(0));

        ListaDesejos updated = service.togglePreReserva("email@test.com", 10L);

        assertNotNull(updated);
        assertTrue(updated.isPreReservaAtiva());
        verify(listaDesejosRepository).save(desejo);
    }

    @Test
    void togglePreReserva_quandoNaoPertenceAoCliente_deveLancarRuntimeException() {
        Cliente c = new Cliente();
        c.setId(1L);

        Cliente outro = new Cliente();
        outro.setId(2L);

        ListaDesejos desejo = new ListaDesejos();
        desejo.setId(10L);
        desejo.setCliente(outro);
        desejo.setIsbn("ISBN");

        when(clienteRepository.findByEmail("email@test.com")).thenReturn(Optional.of(c));
        when(listaDesejosRepository.findById(10L)).thenReturn(Optional.of(desejo));

        assertThrows(RuntimeException.class,
                () -> service.togglePreReserva("email@test.com", 10L));

        verify(listaDesejosRepository, never()).save(any());
    }

    @Test
    void adicionarDesejo_fluxoFeliz_deveSalvarERetornar() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setEmail("email@test.com");

        when(clienteRepository.findByEmail("email@test.com")).thenReturn(Optional.of(c));
        when(listaDesejosRepository.existsByClienteIdAndIsbn(1L, "ISBN")).thenReturn(false);
        when(listaDesejosRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ListaDesejos result = service.adicionarDesejo("email@test.com", "ISBN");

        assertNotNull(result);
        assertEquals("ISBN", result.getIsbn());
        verify(listaDesejosRepository).save(any());
    }

    @Test
    void removerDesejo_fluxoFeliz_deveRemover() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setEmail("email@test.com");

        ListaDesejos desejo = new ListaDesejos();
        desejo.setId(10L);
        desejo.setCliente(c);
        desejo.setIsbn("ISBN");

        when(clienteRepository.findByEmail("email@test.com")).thenReturn(Optional.of(c));
        when(listaDesejosRepository.findById(10L)).thenReturn(Optional.of(desejo));

        service.removerDesejo("email@test.com", 10L);

        verify(listaDesejosRepository).delete(desejo);
    }

    @Test
    void removerDesejo_quandoClienteNaoExiste_deveLancarExcecao() {
        when(clienteRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.removerDesejo("email@test.com", 1L));
    }

    @Test
    void removerDesejo_quandoDesejoNaoExiste_deveLancarExcecao() {
        Cliente c = new Cliente();
        c.setId(1L);

        when(clienteRepository.findByEmail(any())).thenReturn(Optional.of(c));
        when(listaDesejosRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.removerDesejo("email@test.com", 1L));
    }

    @Test
    void removerDesejo_quandoNaoPertenceAoCliente_deveLancarExcecao() {
        Cliente c = new Cliente();
        c.setId(1L);

        Cliente outro = new Cliente();
        outro.setId(2L);

        ListaDesejos desejo = new ListaDesejos();
        desejo.setCliente(outro);

        when(clienteRepository.findByEmail(any())).thenReturn(Optional.of(c));
        when(listaDesejosRepository.findById(1L)).thenReturn(Optional.of(desejo));

        assertThrows(RuntimeException.class,
                () -> service.removerDesejo("email@test.com", 1L));
    }

    @Test
    void listarDesejos_fluxoFeliz_deveRetornarLista() {
        Cliente c = new Cliente();
        c.setId(1L);

        when(clienteRepository.findByEmail(any())).thenReturn(Optional.of(c));
        when(listaDesejosRepository.findByClienteId(1L)).thenReturn(List.of(new ListaDesejos()));

        var lista = service.listarDesejos("email@test.com");

        assertNotNull(lista);
        assertEquals(1, lista.size());
    }

    @Test
    void listarDesejos_quandoClienteNaoExiste_deveLancarExcecao() {
        when(clienteRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.listarDesejos("email@test.com"));
    }

    @Test
    void notificarDisponivel_quandoListaVazia_naoFazNada() {
        when(listaDesejosRepository.findByIsbn("ISBN")).thenReturn(List.of());

        service.notificarClientesSeDisponivel("ISBN", "Livro");

        verifyNoInteractions(emailFacade);
        verifyNoInteractions(notificacaoService);
    }

    @Test
    void notificarDisponivel_fluxoFeliz_deveEnviarEmailENotificacao() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setEmail("email@test.com");
        c.setNome("Nome");

        ListaDesejos desejo = new ListaDesejos();
        desejo.setCliente(c);
        desejo.setIsbn("ISBN");

        when(listaDesejosRepository.findByIsbn("ISBN")).thenReturn(List.of(desejo));

        service.notificarClientesSeDisponivel("ISBN", "Livro");

        verify(emailFacade).sendHtmlSafe(any(), any(), any());
        verify(notificacaoService).criarNotificacaoDashboard(any(), any(), any());
    }

    @Test
    void notificarDisponivel_quandoErroNaoDeveQuebrarLoop() {
        Cliente c = new Cliente();
        c.setEmail("email@test.com");

        ListaDesejos desejo = new ListaDesejos();
        desejo.setCliente(c);

        when(listaDesejosRepository.findByIsbn("ISBN")).thenReturn(List.of(desejo));

        doThrow(new RuntimeException())
                .when(emailFacade)
                .sendHtmlSafe(any(), any(), any());

        assertDoesNotThrow(() -> service.notificarClientesSeDisponivel("ISBN", "Livro"));
    }

    @Test
    void notificarPromocao_quandoListaVazia_naoFazNada() {
        when(listaDesejosRepository.findByIsbn("ISBN")).thenReturn(List.of());

        service.notificarClientesSeEmPromocao("ISBN", "Livro", 10.0);

        verifyNoInteractions(notificacaoService);
    }

    @Test
    void notificarPromocao_fluxoFeliz_deveEnviarNotificacao() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setEmail("email@test.com");

        ListaDesejos desejo = new ListaDesejos();
        desejo.setCliente(c);

        when(listaDesejosRepository.findByIsbn("ISBN")).thenReturn(List.of(desejo));

        service.notificarClientesSeEmPromocao("ISBN", "Livro", 10.0);

        verify(notificacaoService).criarNotificacaoDashboard(any(), any(), any());
    }
}
