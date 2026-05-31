package umc.exs.service.cliente.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.response.cliente.ClienteListaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.CupomUsoRepository;
import umc.exs.repository.negocios.ListaDesejosRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.SolicitacaoCancelamentoRepository;
import umc.exs.repository.negocios.TransacaoRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;

@ExtendWith(MockitoExtension.class)
class ClienteAdminServiceTest {

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    PedidoRepository pedidoRepository;

    @Mock
    TransacaoRepository transacaoRepository;

    @Mock
    CupomUsoRepository cupomUsoRepository;

    @Mock
    TopicoForumRepository topicoForumRepository;

    @Mock
    ListaDesejosRepository listaDesejosRepository;

    @Mock
    LoteRepository loteRepository;

    @Mock
    LivroRepository livroRepository;

    @Mock
    SolicitacaoCancelamentoRepository cancelamentoRepository;

    @InjectMocks
    ClienteAdminService service;

    @Test
    void listarClientes_deveMapearResponses() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Nome");
        c.setEmail("e@test.com");
        c.setDataCriacao(java.time.LocalDateTime.now());
        c.setSaldoTokens(10.0);
        c.setAtivo(true);

        when(clienteRepository.findAll()).thenReturn(List.of(c));

        List<Object[]> stats = new java.util.ArrayList<>();
        stats.add(new Object[] { 1L, 2L, 100.0d });

        when(pedidoRepository.statsGroupedByComprador())
                .thenReturn(stats);

        List<ClienteListaResponse> res = service.listarClientes();
        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).getId());
        assertTrue(res.get(0).isAtivo());
    }

    @Test
    void getPerfilCliente_deveMontarPerfil() {
        Long clienteId = 1L;
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setNome("Nome");
        cliente.setEmail("e@test.com");
        cliente.setCpf("111.222.333-44");
        cliente.setDatanasc(java.time.LocalDate.now());
        cliente.setDataCriacao(java.time.LocalDateTime.now().minusDays(10));
        cliente.setSaldoTokens(10.0);
        cliente.setAtivo(true);

        when(clienteRepository.findById(clienteId)).thenReturn(java.util.Optional.of(cliente));

        when(pedidoRepository.findByCompradorIdOrderByDataCompraDesc(clienteId)).thenReturn(List.of(new Pedido()));
        when(pedidoRepository.sumGastoByClienteId(clienteId)).thenReturn(200.0d);

        when(transacaoRepository.sumValorConfirmadoByClienteId(clienteId)).thenReturn(50.0d);
        when(cupomUsoRepository.countByClienteId(clienteId)).thenReturn(3L);
        when(cancelamentoRepository.countByClienteId(clienteId)).thenReturn(1L);
        when(livroRepository.countByVendedorIdAndAprovadoTrue(clienteId)).thenReturn(2L);
        when(livroRepository.countRejeitadosByVendedorId(clienteId)).thenReturn(0L);
        when(loteRepository.countByClienteIdAndStatusIn(eq(clienteId), anyList())).thenReturn(1L);
        when(topicoForumRepository.countByAutorId(clienteId)).thenReturn(4L);
        when(listaDesejosRepository.countByClienteId(clienteId)).thenReturn(5L);

        ClientePerfilResponse res = service.getPerfilCliente(clienteId);
        assertNotNull(res);
        assertEquals(clienteId, res.getId());
        assertEquals(1, res.getTotalPedidos());
    }
}
