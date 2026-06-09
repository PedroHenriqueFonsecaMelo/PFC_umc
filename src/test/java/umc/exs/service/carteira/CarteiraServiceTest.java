package umc.exs.service.carteira;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.TransacaoRepository;
import umc.exs.service.carteira.delegado.CarteiraEmailService;
import umc.exs.service.carteira.delegado.CarteiraNotificacaoService;
import umc.exs.service.cliente.delegado.ClienteRepositoryService;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class CarteiraServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private ClienteRepositoryService clienteRepositoryService;

    @Mock
    private CarteiraEmailService carteiraEmailService;

    @Mock
    private CarteiraNotificacaoService carteiraNotificacaoService;

    @Mock
    private LogAuditoriaService auditoria;

    @InjectMocks
    private CarteiraService service;

    private Cliente cliente(String email, double saldo) {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setEmail(email);
        c.setNome("Nome");
        c.setSaldoTokens(saldo);
        return c;
    }

    @Test
    void adicionarTokens_deveSomarSaldoERegistrarTransacao() {
        Cliente cliente = cliente("c@test.com", 10.0);

        service.adicionarTokens(cliente, 5.0, "PIX", "info");

        assertEquals(15.0, cliente.getSaldoTokens());
        verify(transacaoRepository).save(any(Transacao.class));
        verify(clienteRepositoryService).salvar(cliente);
        verify(carteiraNotificacaoService).notificarRecarga(cliente, 5.0, "PIX");
        verify(auditoria).registrarLog(eq("CARTEIRA_TOKEN_ADICIONADO"), eq(cliente.getId()), eq(cliente.getEmail()),
                contains("PIX"));
        verify(carteiraEmailService).enviarCredito(cliente, 10.0, 5.0, "PIX");
    }

    @Test
    void debitarTokens_saldoInsuficiente_deveLancar() {
        Cliente cliente = cliente("c@test.com", 2.0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.debitarTokens(cliente, 5.0, "desc"));
        assertEquals("Saldo insuficiente.", ex.getMessage());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void debitarTokens_deveDebitarERegistrar() {
        Cliente cliente = cliente("c@test.com", 10.0);

        service.debitarTokens(cliente, 4.0, "desc");

        assertEquals(6.0, cliente.getSaldoTokens());
        verify(transacaoRepository).save(any(Transacao.class));
        verify(clienteRepositoryService).salvar(cliente);
        verify(carteiraNotificacaoService).notificarDebito(cliente, 4.0, "desc");
        verify(auditoria).registrarLog(
                eq("CARTEIRA_TOKEN_DEBITADO"),
                eq(cliente.getId()),
                eq(cliente.getEmail()),
                contains("desc"));
        verify(carteiraEmailService).enviarDebito(cliente, 10.0, 4.0, "desc");
    }

    @Test
    void registrarIntencaoPagamento_deveSalvarTransacaoPendente() {
        Cliente cliente = cliente("c@test.com", 10.0);
        service.registrarIntencaoPagamento(cliente, 9.0, "pg-1");
        verify(transacaoRepository).save(any(Transacao.class));
    }

    @Test
    void confirmarPagamentoPix_transacaoPendente_deveConcluirESomarSaldo() {
        Cliente cliente = cliente("c@test.com", 10.0);

        Transacao t = Transacao.builder()
                .cliente(cliente)
                .valor(3.0)
                .pagamentoId("pg")
                .status("PENDENTE")
                .metodoPagamento("PIX")
                .dataHora(LocalDateTime.now())
                .build();

        when(transacaoRepository.findByPagamentoId("pg")).thenReturn(t);

        service.confirmarPagamentoPix("pg");

        assertEquals(13.0, cliente.getSaldoTokens());
        verify(transacaoRepository).save(t);
        verify(clienteRepositoryService).salvar(cliente);
        verify(carteiraEmailService).enviarConfirmacaoPix(cliente, 10.0, 3.0);
        verify(carteiraNotificacaoService).notificarPixConfirmado(cliente, 3.0);
    }

    @Test
    void confirmarPagamentoPix_transacaoNaoExiste_deveLancar() {
        when(transacaoRepository.findByPagamentoId("pg")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.confirmarPagamentoPix("pg"));
    }

    @Test
    void confirmarPagamentoPix_transacaoJaConcluida_deveRetornarSemAtualizar() {
        Cliente cliente = cliente("c@test.com", 10.0);

        Transacao t = Transacao.builder()
                .cliente(cliente)
                .valor(3.0)
                .pagamentoId("pg")
                .status("CONCLUIDO")
                .build();

        when(transacaoRepository.findByPagamentoId("pg")).thenReturn(t);

        service.confirmarPagamentoPix("pg");

        verify(transacaoRepository, never()).save(any());
        verify(carteiraEmailService, never()).enviarConfirmacaoPix(any(), anyDouble(), anyDouble());
    }

    @Test
    void listarHistoricoPorCliente_deveDelegar() {
        List<Transacao> lista = List.of(mock(Transacao.class));
              
        when(transacaoRepository.findByClienteIdAndStatusOrderByDataHoraDesc(any(), any()))
                .thenReturn(lista);
        assertEquals(lista, service.listarHistoricoPorCliente(1L));
    }

    @Test
    void verificarStatusPagamento_trueQuandoConcluido() {
        Cliente cliente = cliente("c@test.com", 0.0);
        Transacao t = Transacao.builder().cliente(cliente).pagamentoId("pg").status("CONCLUIDO").build();
        when(transacaoRepository.findByPagamentoId("pg")).thenReturn(t);
        assertTrue(service.verificarStatusPagamento("pg"));
    }

    @Test
    void verificarStatusPagamento_falseQuandoInexistente() {
        when(transacaoRepository.findByPagamentoId("pg")).thenReturn(null);
        assertFalse(service.verificarStatusPagamento("pg"));
    }
}
