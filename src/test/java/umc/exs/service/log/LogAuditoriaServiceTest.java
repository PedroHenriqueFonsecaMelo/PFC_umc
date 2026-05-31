package umc.exs.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.logic.LogAuditoriaRepository;
import umc.exs.repository.usuario.ClienteRepository;

@ExtendWith(MockitoExtension.class)
class LogAuditoriaServiceTest {

    @Mock
    LogAuditoriaRepository repository;

    @Mock
    ClienteRepository clienteRepository;

    @InjectMocks
    LogAuditoriaService service;

    @Test
    void registrarLog_devePersistirQuandoOk() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("a@test.com");

        service.registrarLog("ACAO", 1L, "a@test.com", "detalhes");

        verify(repository).save(any(LogAuditoria.class));
    }

    @Test
    void buscarLogsDoCliente_quandoClienteExiste_deveChamarRepository() {
        Long id = 1L;
        Cliente c = new Cliente();
        c.setId(id);
        c.setDataCriacao(LocalDateTime.now().minusDays(10));

        when(clienteRepository.findById(id)).thenReturn(Optional.of(c));
        when(repository.findByIdUsuarioAndDataHoraAfterOrderByDataHoraDesc(id, c.getDataCriacao()))
                .thenReturn(List.of(new LogAuditoria()));

        List<LogAuditoria> logs = service.buscarLogsDoCliente(id);
        assertEquals(1, logs.size());
        verify(repository).findByIdUsuarioAndDataHoraAfterOrderByDataHoraDesc(id, c.getDataCriacao());
    }

    @Test
    void buscarTodosLogs_deveDelegar() {
        when(repository.findAllByOrderByDataHoraDesc()).thenReturn(List.of(new LogAuditoria()));
        assertEquals(1, service.buscarTodosLogs().size());
        verify(repository).findAllByOrderByDataHoraDesc();
    }

    @Test
    void exportarCSV_deveGerarStringComCabeçalho() {
        LogAuditoria l = new LogAuditoria();
        l.setId(1L);
        l.setAcao("ACAO");
        l.setEmailUsuario("email@test.com");
        l.setIdUsuario(2L);
        l.setDetalhes("det");
        l.setDataHora(LocalDateTime.now());

        String csv = service.exportarCSV(List.of(l));
        assertNotNull(csv);
        assertTrue(csv.contains("ID,Acao"));
        assertTrue(csv.contains("ACAO"));
    }

    @Test
    void buscarComFiltros_deveConverterDatasETriarChamada() {
        when(repository.buscarComFiltros(any(), any(), any(), any()))
                .thenReturn(List.of(new LogAuditoria()));

        List<LogAuditoria> logs = service.buscarComFiltros(
                "email@test.com",
                "ACAO",
                "2024-01-01",
                "2024-01-02");

        assertEquals(1, logs.size());
        verify(repository).buscarComFiltros(any(), any(), any(), any());
    }
}
