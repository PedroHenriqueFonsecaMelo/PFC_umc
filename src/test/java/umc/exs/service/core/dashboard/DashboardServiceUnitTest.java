package umc.exs.service.core.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.response.admin.DashboardResponse;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.VisitaSiteRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.TransacaoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.log.AppLogger;

@ExtendWith(MockitoExtension.class)
class DashboardServiceUnitTest {

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    LivroRepository livroRepository;

    @Mock
    PedidoRepository pedidoRepository;

    @Mock
    TransacaoRepository transacaoRepository;

    @Mock
    VisitaSiteRepository visitaSiteRepository;

    @Mock
    AppLogger appLogger;

    @InjectMocks
    DashboardService service;

    @BeforeEach
    void setUp() {
        // nada
    }

    @Test
    void getMetricas_quandoRepositoriosRetornamValores_deveMontarDashboard() {
        when(clienteRepository.count()).thenReturn(10L);
        when(livroRepository.count()).thenReturn(3L);
        when(visitaSiteRepository.sumTotalVisitas()).thenReturn(100L);
        when(pedidoRepository.count()).thenReturn(2L);
        when(clienteRepository.sumSaldoTokensAtivos()).thenReturn(50.0);
        when(pedidoRepository.sumTokensUtilizados()).thenReturn(20.0);
        when(transacaoRepository.count()).thenReturn(7L);
        when(transacaoRepository.countByStatus("CONFIRMADO")).thenReturn(4L);

        when(clienteRepository.findByDataCriacaoAfter(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(pedidoRepository.findDataCompraAfterProjection(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(livroRepository.findDataAnuncioAfterProjection(any(LocalDateTime.class)))
                .thenReturn(List.of());

        DashboardResponse resp = service.getMetricas();

        assertNotNull(resp);
        assertEquals(10L, resp.getTotalClientes());
        assertEquals(3L, resp.getTotalLivros());
        assertEquals(100L, resp.getTotalVisitas());
        assertEquals(2L, resp.getTotalAdquiridos());
        assertEquals(50.0, resp.getTokensDisponibilizados());
        assertEquals(20.0, resp.getTokensUtilizados());
    }
}

