package umc.exs.service.core.livros.delegado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.dashboard.PedidoService;
import umc.exs.service.cupom.CupomService;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivroCompraServiceTest {

        @Mock
        private LivroRepository livroRepository;
        @Mock
        private ClienteRepository clienteRepository;
        @Mock
        private EmailFacade emailFacade;
        @Mock
        private PedidoService pedidoService;
        @Mock
        private CupomService cupomService;
        @Mock
        private GamificacaoService gamificacaoService;
        @Mock
        private LogAuditoriaService logAuditoria;
        @Mock
        private NotificacaoService notificacaoService;

        @InjectMocks
        private LivroCompraService service;

        private Cliente cliente;
        private Livro livro;

        @BeforeEach
        void setup() {
                cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("teste@email.com");
                cliente.setNome("Cliente Teste");
                cliente.setSaldoTokens(100.0);
                cliente.setEnderecos(Set.of(new Endereco()));

                livro = new Livro();
                livro.setId(10L);
                livro.setTitulo("Livro Teste");
                livro.setPrecoAprovado(50.0);
                livro.setAprovado(true);
        }

        @Test
        void deveRealizarCompraComSucesso() {

                when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                                .thenReturn(Optional.of(livro));

                when(clienteRepository.findByEmail("teste@email.com"))
                                .thenReturn(Optional.of(cliente));

                when(pedidoService.registrarPedido(any(), any(), anyString()))
                                .thenReturn(null); // evita NullPointer interno

                when(notificacaoService.criarNotificacaoDashboard(any(), anyString(), anyString()))
                                .thenReturn(null);

                doNothing().when(emailFacade)
                                .sendHtmlSafe(anyString(), anyString(), anyString());

                service.realizarCompra(10L, "teste@email.com");

                assertEquals(50.0, cliente.getSaldoTokens());

                verify(livroRepository).save(any());
                verify(pedidoService).registrarPedido(any(), any(), anyString());
                verify(logAuditoria).registrarLog(anyString(), anyLong(), anyString(), anyString());
                verify(gamificacaoService).xpCompra(anyLong());
        }

        @Test
        void deveFalharQuandoLivroNaoExiste() {

                when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                                .thenReturn(Optional.empty());

                assertThrows(IllegalArgumentException.class,
                                () -> service.realizarCompra(10L, "teste@email.com"));
        }

        @Test
        void deveFalharQuandoClienteNaoExiste() {

                when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                                .thenReturn(Optional.of(livro));

                when(clienteRepository.findByEmail("teste@email.com"))
                                .thenReturn(Optional.empty());

                assertThrows(IllegalStateException.class,
                                () -> service.realizarCompra(10L, "teste@email.com"));
        }

        @Test
        void deveFalharQuandoSaldoInsuficiente() {

                cliente.setSaldoTokens(10.0);

                when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                                .thenReturn(Optional.of(livro));

                when(clienteRepository.findByEmail("teste@email.com"))
                                .thenReturn(Optional.of(cliente));

                assertThrows(IllegalStateException.class,
                                () -> service.realizarCompra(10L, "teste@email.com"));
        }

        @Test
        void deveFalharQuandoNaoTemEndereco() {

                cliente.setEnderecos(Set.of());

                when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                                .thenReturn(Optional.of(livro));

                when(clienteRepository.findByEmail("teste@email.com"))
                                .thenReturn(Optional.of(cliente));

                assertThrows(IllegalStateException.class,
                                () -> service.realizarCompra(10L, "teste@email.com"));
        }

        @Test
        void comprarCarrinho_deveRealizarCompraComSucesso() {

                Livro livro1 = new Livro();
                livro1.setId(1L);
                livro1.setTitulo("Livro 1");
                livro1.setPrecoAprovado(30.0);
                livro1.setAprovado(true);

                Livro livro2 = new Livro();
                livro2.setId(2L);
                livro2.setTitulo("Livro 2");
                livro2.setPrecoAprovado(20.0);
                livro2.setAprovado(true);

                CarrinhoCompraRequest request = new CarrinhoCompraRequest();
                request.setLivroIds(List.of(1L, 2L));

                when(clienteRepository.findByEmail(anyString()))
                                .thenReturn(Optional.of(cliente));

                when(livroRepository.findAllDisponiveisWithLock(anyList()))
                                .thenReturn(List.of(livro1, livro2));

                when(pedidoService.gerarCodigoPedido())
                                .thenReturn("COD123");

                when(pedidoService.registrarPedido(any(), any(), anyString()))
                                .thenAnswer(invocation -> {
                                        Pedido p = new Pedido();
                                        p.setId(1L);
                                        p.setCodigoPedido("COD123");
                                        return p;
                                });

                CarrinhoCompraResponse response = service.comprarCarrinho("teste@email.com", request);

                assertEquals(2, response.getTotalComprados());
                assertEquals(50.0, response.getTotalOriginal());
                assertEquals(50.0, response.getTotalGasto());
                assertEquals(50.0, cliente.getSaldoTokens());

                verify(clienteRepository).save(any());
        }

        @Test
        void comprarCarrinho_deveFalharQuandoExcedeLimite() {

                CarrinhoCompraRequest request = new CarrinhoCompraRequest();
                request.setLivroIds(
                                java.util.stream.LongStream.range(1, 25)
                                                .boxed().toList());

                when(clienteRepository.findByEmail(anyString()))
                                .thenReturn(Optional.of(cliente));

                assertThrows(IllegalArgumentException.class,
                                () -> service.comprarCarrinho("teste@email.com", request));
        }

        @Test
        void comprarCarrinho_deveFalharQuandoSaldoInsuficiente() {

                cliente.setSaldoTokens(10.0);

                Livro livro = new Livro();
                livro.setId(1L);
                livro.setPrecoAprovado(50.0);
                livro.setAprovado(true);

                CarrinhoCompraRequest request = new CarrinhoCompraRequest();
                request.setLivroIds(List.of(1L));

                when(clienteRepository.findByEmail(anyString()))
                                .thenReturn(Optional.of(cliente));

                when(livroRepository.findAllDisponiveisWithLock(anyList()))
                                .thenReturn(List.of(livro));

                assertThrows(IllegalStateException.class,
                                () -> service.comprarCarrinho("teste@email.com", request));
        }

        @Test
        void comprarCarrinho_deveAplicarCupom() {

                Livro livro = new Livro();
                livro.setId(1L);
                livro.setPrecoAprovado(100.0);
                livro.setAprovado(true);
                livro.setEmPromocao(false);

                CarrinhoCompraRequest request = new CarrinhoCompraRequest();
                request.setLivroIds(List.of(1L));
                request.setCodigoCupom("DESC10");

                when(clienteRepository.findByEmail(anyString()))
                                .thenReturn(Optional.of(cliente));

                when(livroRepository.findAllDisponiveisWithLock(anyList()))
                                .thenReturn(List.of(livro));

                when(cupomService.aplicarCupomCarrinho(anyString(), any(), anyDouble()))
                                .thenReturn(80.0);

                when(pedidoService.gerarCodigoPedido())
                                .thenReturn("COD123");

                when(pedidoService.registrarPedido(any(), any(), anyString()))
                                .thenAnswer(invocation -> {
                                        Pedido p = new Pedido();
                                        p.setId(1L);
                                        p.setCodigoPedido("COD123");
                                        return p;
                                });

                CarrinhoCompraResponse response = service.comprarCarrinho("teste@email.com", request);

                assertEquals(20.0, response.getDescontoAplicado());
                assertEquals("DESC10", response.getCodigoCupomAplicado());
        }

        @Test
        void comprarCarrinho_deveFalharQuandoNenhumLivroDisponivel() {

                CarrinhoCompraRequest request = new CarrinhoCompraRequest();
                request.setLivroIds(List.of(1L));

                when(clienteRepository.findByEmail(anyString()))
                                .thenReturn(Optional.of(cliente));

                when(livroRepository.findAllDisponiveisWithLock(anyList()))
                                .thenReturn(List.of());

                assertThrows(IllegalArgumentException.class,
                                () -> service.comprarCarrinho("teste@email.com", request));
        }
}