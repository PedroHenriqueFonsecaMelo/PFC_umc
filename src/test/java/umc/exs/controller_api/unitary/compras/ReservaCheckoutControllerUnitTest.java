package umc.exs.controller_api.unitary.compras;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import umc.exs.controller.api.compras.ReservaCheckoutController;
import umc.exs.service.core.dashboard.ReservaCheckoutService;

class ReservaCheckoutControllerUnitTest {

    private ReservaCheckoutService service;
    private ReservaCheckoutController controller;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        service = mock(ReservaCheckoutService.class);
        controller = new ReservaCheckoutController(service);
        user = User.withUsername("cliente@email.com")
                .password("pass")
                .authorities("USER")
                .build();
    }

    @Test
    void reservar_SemAuth_Retorna401() {
        ResponseEntity<Map<String, Object>> resp = controller.reservar(Map.of("livroIds", List.of(1)), null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void reservar_SemLivros_Retorna400() {
        ResponseEntity<Map<String, Object>> resp = controller.reservar(Map.of("livroIds", List.of()), user);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(false, resp.getBody().get("reservado"));
        verifyNoInteractions(service);
    }

    @Test
    void status_ComSucesso_Retorna200() {
        when(service.statusReserva(eq(5L), eq(user.getUsername())))
                .thenReturn(Map.of("ok", true));

        ResponseEntity<Map<String, Object>> resp = controller.status(5L, user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(true, resp.getBody().get("ok"));
        verify(service).statusReserva(5L, user.getUsername());
    }
}

