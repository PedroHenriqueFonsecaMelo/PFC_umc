package umc.exs.service.core.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
}

