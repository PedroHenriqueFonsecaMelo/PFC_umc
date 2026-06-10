package umc.exs.service.core.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import umc.exs.model.entidades.foundation.ReservaCheckout;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.ReservaCheckoutRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.log.AppLogger;

class ReservaCheckoutServiceTest {

    private ReservaCheckoutRepository reservaRepo;
    private ClienteRepository clienteRepo;
    private AppLogger appLogger;

    private ReservaCheckoutService service;

    @BeforeEach
    void setup() {
        reservaRepo = mock(ReservaCheckoutRepository.class);
        clienteRepo = mock(ClienteRepository.class);
        appLogger = mock(AppLogger.class);

        service = new ReservaCheckoutService(
                reservaRepo,
                clienteRepo,
                appLogger
        );
    }

    // =========================================
    // RESERVAR - LIMITE EXCEDIDO
    // =========================================
    @Test
    void reservar_limiteExcedido() {
        Map<String, Object> result =
                service.reservar(List.of(1L,2L,3L,4L,5L,6L), "email");

        assertEquals(false, result.get("reservado"));
        assertEquals("LIMITE_EXCEDIDO", result.get("motivo"));
    }

    // =========================================
    // RESERVAR - NAO AUTENTICADO
    // =========================================
    @Test
    void reservar_naoAutenticado() {
        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.empty());

        Map<String, Object> result =
                service.reservar(List.of(1L), "email");

        assertEquals(false, result.get("reservado"));
        assertEquals("NAO_AUTENTICADO", result.get("motivo"));
    }

    // =========================================
    // RESERVAR - BLOQUEADO
    // =========================================
    @Test
    void reservar_bloqueado() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        ReservaCheckout r = new ReservaCheckout();
        r.setBloqueadoAte(LocalDateTime.now().plusMinutes(5));

        when(reservaRepo.findByLivroIdAndClienteId(1L, 1L))
                .thenReturn(Optional.of(r));

        Map<String, Object> result =
                service.reservar(List.of(1L), "email");

        assertEquals(false, result.get("reservado"));
        assertEquals("BLOQUEADO", result.get("motivo"));
    }

    // =========================================
    // RESERVAR - INDISPONIVEL
    // =========================================
    @Test
    void reservar_indisponivel() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        when(reservaRepo.findByLivroIdAndClienteId(any(), any()))
                .thenReturn(Optional.empty());

        when(reservaRepo.findReservaAtivaDeOutro(any(), any(), any()))
                .thenReturn(Optional.of(new ReservaCheckout()));

        Map<String, Object> result =
                service.reservar(List.of(1L), "email");

        assertEquals(false, result.get("reservado"));
        assertEquals("INDISPONIVEL", result.get("motivo"));
    }

    // =========================================
    // RESERVAR - SUCESSO
    // =========================================
    @Test
    void reservar_sucesso() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        when(reservaRepo.findByLivroIdAndClienteId(any(), any()))
                .thenReturn(Optional.empty());

        when(reservaRepo.findReservaAtivaDeOutro(any(), any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result =
                service.reservar(List.of(1L), "email");

        assertEquals(true, result.get("reservado"));

        verify(reservaRepo).save(any());
        verify(appLogger).success(any(), any(), any(), any());
    }

    // =========================================
    // LIBERAR RESERVA - REMOVE
    // =========================================
    @Test
    void liberarReservas_removeReserva() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        ReservaCheckout r = new ReservaCheckout();
        r.setTentativas(0);

        when(reservaRepo.findByLivroIdAndClienteId(1L, 1L))
                .thenReturn(Optional.of(r));

        Map<String, Object> result =
                service.liberarReservas(List.of(1L), "email");

        assertEquals(true, result.get("liberado"));

        verify(reservaRepo).delete(r);
    }

    // =========================================
    // LIBERAR RESERVA - BLOQUEIA
    // =========================================
    @Test
    void liberarReservas_bloqueiaAposLimite() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        ReservaCheckout r = new ReservaCheckout();
        r.setTentativas(2);

        when(reservaRepo.findByLivroIdAndClienteId(1L, 1L))
                .thenReturn(Optional.of(r));

        service.liberarReservas(List.of(1L), "email");

        verify(reservaRepo).save(r);
        assertNotNull(r.getBloqueadoAte());
    }

    // =========================================
    // STATUS - NAO AUTENTICADO
    // =========================================
    @Test
    void statusReserva_naoAutenticado() {

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.empty());

        Map<String, Object> result =
                service.statusReserva(1L, "email");

        assertEquals(false, result.get("ativa"));
    }

    // =========================================
    // STATUS - NAO EXISTE
    // =========================================
    @Test
    void statusReserva_naoExiste() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        when(reservaRepo.findByLivroIdAndClienteId(1L, 1L))
                .thenReturn(Optional.empty());

        Map<String, Object> result =
                service.statusReserva(1L, "email");

        assertEquals(false, result.get("ativa"));
    }

    // =========================================
    // STATUS - EXPIRADO
    // =========================================
    @Test
    void statusReserva_expirado() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        ReservaCheckout r = new ReservaCheckout();
        r.setExpiraEm(LocalDateTime.now().minusMinutes(1));

        when(reservaRepo.findByLivroIdAndClienteId(1L, 1L))
                .thenReturn(Optional.of(r));

        Map<String, Object> result =
                service.statusReserva(1L, "email");

        assertEquals(false, result.get("ativa"));
    }

    // =========================================
    // STATUS - ATIVO
    // =========================================
    @Test
    void statusReserva_ativo() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(clienteRepo.findByEmail("email"))
                .thenReturn(Optional.of(cliente));

        ReservaCheckout r = new ReservaCheckout();
        r.setExpiraEm(LocalDateTime.now().plusMinutes(5));

        when(reservaRepo.findByLivroIdAndClienteId(1L, 1L))
                .thenReturn(Optional.of(r));

        Map<String, Object> result =
                service.statusReserva(1L, "email");

        assertEquals(true, result.get("ativa"));
        assertNotNull(result.get("segundosRestantes"));
    }

    // =========================================
    // LIMPEZA
    // =========================================
    @Test
    void limparReservasExpiradas_deveExecutar() {

        service.limparReservasExpiradas();

        verify(reservaRepo).deleteExpiradas(any());
    }
}
