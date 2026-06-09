package umc.exs.controller_api.unitary.control;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import umc.exs.controller.api.control.ReporteController;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.logic.ReporteRepository;
import umc.exs.repository.usuario.ClienteRepository;

class ReporteControllerUnitTest {

    private ClienteRepository clienteRepository;
    private ReporteRepository reporteRepository;
    private ReporteController controller;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        reporteRepository = mock(ReporteRepository.class);

        controller = new ReporteController(
                clienteRepository,
                reporteRepository
        );
    }

    @Test
    void receberReporte_SemClienteRetornaOk_semSalvar() {
        when(clienteRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.receberReporte(Map.of(
                "motivo", "ABUSO",
                "email", "x@y.com",
                "detalhes", "det"));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(reporteRepository).save(any());
        assertEquals(true, ((Map<?, ?>) resp.getBody()).get("ok"));
    }

    @Test
    void receberReporte_ComClienteSalvaReporte() {
        Cliente cliente = mock(Cliente.class);

        when(clienteRepository.findByEmail("x@y.com"))
                .thenReturn(Optional.of(cliente));

        ResponseEntity<?> resp = controller.receberReporte(Map.of(
                "motivo", "ABUSO",
                "email", "x@y.com",
                "detalhes", "detalhes grandes"));

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(reporteRepository).save(any());

        assertEquals(true, ((Map<?, ?>) resp.getBody()).get("ok"));
    }
}
