package umc.exs.controller_api.unitary.compras;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.mapstruct.factory.Mappers;

import umc.exs.controller.api.compras.PedidoController;
import umc.exs.dto.mapper.PedidoMapper;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.dashboard.PedidoService;
import umc.exs.service.storage.EtiquetaService;

class PedidoControllerUnitTest {

    private PedidoService pedidoService;
    private ClienteService clienteService;
    private EtiquetaService etiquetaService;
    private PedidoMapper pedidoMapper;
    private PedidoController controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        pedidoService = mock(PedidoService.class);
        clienteService = mock(ClienteService.class);
        etiquetaService = mock(EtiquetaService.class);
        pedidoMapper = Mappers.getMapper(PedidoMapper.class);

        controller = new PedidoController(pedidoService, clienteService, pedidoMapper, etiquetaService);

        user = User.withUsername("cliente@email.com")
                .password("pass")
                .authorities("USER")
                .build();
    }

    @Test
    void listarPendentes_SemAuth_Retorna401() {
        ResponseEntity<?> resp = controller.listarPendentes(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void listarPendentes_ComSucesso_Retorna200() {
        Cliente cliente = mock(Cliente.class);
        when(clienteService.buscarClientePorEmail(eq(user.getUsername())))
                .thenReturn(java.util.Optional.of(cliente));
        when(cliente.getId()).thenReturn(1L);

        when(pedidoService.listarPendentes(eq(1L)))
                .thenReturn(List.of());

        ResponseEntity<?> resp = controller.listarPendentes(user);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(pedidoService).listarPendentes(1L);
    }
}

