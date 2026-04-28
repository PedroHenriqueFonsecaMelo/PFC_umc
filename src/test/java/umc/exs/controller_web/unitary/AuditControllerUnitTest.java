package umc.exs.controller_web.unitary;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.controller.web.AuditController;
import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.log.LogAuditoriaService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditControllerUnitTest {

    private ClienteService clienteService;
    private LogAuditoriaService logAuditoriaService;
    private AuditController controller;

    @BeforeEach
    void setUp() {
        clienteService = mock(ClienteService.class);
        logAuditoriaService = mock(LogAuditoriaService.class);
        controller = new AuditController(clienteService, logAuditoriaService);
    }

    @Test
    void deveRedirecionarParaLoginQuandoPrincipalNull() {
        Model model = new ExtendedModelMap();

        String view = controller.mostrarAuditoria(null, model);

        assertEquals("redirect:/clientes/login", view);
    }

    @Test
    void deveRetornarLogsJsonQuandoPrincipalValido() {
        Principal principal = () -> "user@example.com";
        ClienteDTO clienteDTO = new ClienteDTO();
        clienteDTO.setId(10L);
        clienteDTO.setEmail("user@example.com");

        List<LogAuditoria> logs = List.of(new LogAuditoria());
        when(clienteService.buscarClientePorEmail("user@example.com")).thenReturn(Optional.of(clienteDTO));
        when(logAuditoriaService.buscarLogsDoCliente(10L)).thenReturn(logs);

        List<LogAuditoria> result = controller.listarLogsJson(principal);

        assertSame(logs, result);
    }
}
