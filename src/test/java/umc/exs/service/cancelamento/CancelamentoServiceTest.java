package umc.exs.service.cancelamento;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.request.admin.CancelamentoRequest;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.MotivoCategoria;
import umc.exs.model.enums.StatusEnvio;
import umc.exs.model.enums.StatusSolicitacao;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.SolicitacaoCancelamentoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

@ExtendWith(MockitoExtension.class)
class CancelamentoServiceTest {

    @Mock
    SolicitacaoCancelamentoRepository cancelamentoRepository;

    @Mock
    PedidoRepository pedidoRepository;

    @Mock
    LivroRepository livroRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    NotificacaoService notificacaoService;

    @Mock
    EmailFacade emailFacade;

    @Mock
    LogAuditoriaService logAuditoria;

    @InjectMocks
    CancelamentoService service;

    private Pedido criarPedido(Long pedidoId, Cliente comprador, Long livroId, double preco) {
        Pedido p = new Pedido();
        p.setId(pedidoId);
        p.setComprador(comprador);
        p.setLivroId(livroId);
        p.setPrecoLivro(preco);
        p.setTituloLivro("Titulo");
        p.setStatusEnvio(StatusEnvio.AGUARDANDO_ENVIO);
        return p;
    }

    @Test
    void solicitarCancelamento_sucesso_salvaENotifica() {
        Long pedidoId = 1L;
        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setEmail("cliente@test.com");
        cliente.setNome("Cliente");

        Pedido pedido = criarPedido(pedidoId, cliente, 20L, 15.0);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(cancelamentoRepository.existsByPedidoIdAndStatus(pedidoId, StatusSolicitacao.PENDENTE)).thenReturn(false);
        when(cancelamentoRepository.save(any(SolicitacaoCancelamento.class))).thenAnswer(i -> i.getArgument(0));

        CancelamentoRequest req = new CancelamentoRequest();
        req.setMotivoCategoria(MotivoCategoria.OUTRO);
        req.setMotivoDescricao("Quero cancelar");

        SolicitacaoCancelamento sol = service.solicitarCancelamento(pedidoId, cliente.getEmail(), req);

        assertNotNull(sol);
        assertEquals(StatusSolicitacao.PENDENTE, sol.getStatus());
        assertEquals("Quero cancelar", sol.getMotivoDescricao());

        verify(cancelamentoRepository).save(any(SolicitacaoCancelamento.class));
        verify(logAuditoria).registrarLog(eq("CANCELAMENTO_SOLICITADO"), eq(cliente.getId()), eq(cliente.getEmail()),
                contains("Outro"));
        verify(notificacaoService).criarNotificacaoDashboard(eq(cliente), contains("solicitação de cancelamento"),
                anyString());
    }

    @Test
    void solicitarCancelamento_pedidoNaoEncontrado_lanca() {
        when(pedidoRepository.findById(anyLong())).thenReturn(Optional.empty());
        CancelamentoRequest req = new CancelamentoRequest();
        req.setMotivoCategoria(MotivoCategoria.OUTRO);
        req.setMotivoDescricao("x");

        assertThrows(IllegalArgumentException.class,
                () -> service.solicitarCancelamento(1L, "email@test.com", req));
    }

    @Test
    void listarTodas_deveRetornarTudo() {
        SolicitacaoCancelamento a = new SolicitacaoCancelamento();
        List<SolicitacaoCancelamento> lista = List.of(a);
        when(cancelamentoRepository.findAllByOrderByDataSolicitacaoDesc()).thenReturn(lista);

        assertEquals(lista, service.listarTodas());
    }

    @Test
    void listarPendentes_deveRetornarSomentePendentes() {
        SolicitacaoCancelamento a = new SolicitacaoCancelamento();
        List<SolicitacaoCancelamento> lista = List.of(a);
        when(cancelamentoRepository.findByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao.PENDENTE))
                .thenReturn(lista);

        assertEquals(lista, service.listarPendentes());
    }

    @Test
    void contarPendentes_deveRetornarQuantidade() {
        when(cancelamentoRepository.countByStatus(StatusSolicitacao.PENDENTE)).thenReturn(5L);
        assertEquals(5L, service.contarPendentes());
    }

    @Test
    void buscarPorId_deveRetornarOuLancar() {
        Long id = 1L;
        SolicitacaoCancelamento sol = new SolicitacaoCancelamento();
        when(cancelamentoRepository.findById(id)).thenReturn(Optional.of(sol));
        assertSame(sol, service.buscarPorId(id));

        when(cancelamentoRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(2L));
    }

    @Test
    void aprovarCancelamento_deveAtualizarStatusEEstornar() {
        Long solicitacaoId = 1L;
        String comentario = "ok";

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setEmail("cliente@test.com");
        cliente.setNome("Cliente");
        cliente.setSaldoTokens(2.0);

        Pedido pedido = criarPedido(99L, cliente, 50L, 7.5);
        pedido.setDataCompra(LocalDateTime.now());

        SolicitacaoCancelamento sol = new SolicitacaoCancelamento();
        sol.setId(solicitacaoId);
        sol.setStatus(StatusSolicitacao.PENDENTE);
        sol.setPedido(pedido);
        sol.setCliente(cliente);
        sol.setDataResposta(null);

        when(cancelamentoRepository.findById(solicitacaoId)).thenReturn(Optional.of(sol));
        when(cancelamentoRepository.save(any(SolicitacaoCancelamento.class))).thenAnswer(i -> i.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(livroRepository.findById(anyLong())).thenReturn(Optional.of(new Livro()));

        SolicitacaoCancelamento result = service.aprovarCancelamento(solicitacaoId, comentario);
        assertEquals(StatusSolicitacao.APROVADO, result.getStatus());
        assertNotNull(result.getDataResposta());

        verify(clienteRepository).save(argThat(c -> c.getSaldoTokens() > 2.0));
        verify(notificacaoService).criarNotificacaoDashboard(eq(cliente), contains("aprovado"), anyString());
        verify(emailFacade).sendHtmlSafe(
                anyString(),
                anyString(),
                any());
    }

    @Test
    void recusarCancelamento_deveAtualizarParaRecusado() {
        Long solicitacaoId = 1L;
        String comentario = "não";

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setEmail("cliente@test.com");
        cliente.setNome("Cliente");

        Pedido pedido = criarPedido(99L, cliente, 50L, 7.5);

        SolicitacaoCancelamento sol = new SolicitacaoCancelamento();
        sol.setId(solicitacaoId);
        sol.setStatus(StatusSolicitacao.PENDENTE);
        sol.setPedido(pedido);
        sol.setCliente(cliente);

        when(cancelamentoRepository.findById(solicitacaoId)).thenReturn(Optional.of(sol));
        when(cancelamentoRepository.save(any(SolicitacaoCancelamento.class))).thenAnswer(i -> i.getArgument(0));

        SolicitacaoCancelamento result = service.recusarCancelamento(solicitacaoId, comentario);
        assertEquals(StatusSolicitacao.RECUSADO, result.getStatus());
        verify(notificacaoService).criarNotificacaoDashboard(eq(cliente), contains("recusada"), anyString());
    }
}
