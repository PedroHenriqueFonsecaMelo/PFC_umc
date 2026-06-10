package umc.exs.service.core.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import umc.exs.model.entidades.foundation.NotificacaoDashboard;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.StatusEnvio;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.log.AppLogger;
import umc.exs.service.notificacao.NotificacaoService;

class PedidoServiceTest {

        private PedidoRepository pedidoRepository;
        private ClienteRepository clienteRepository;
        private AppLogger appLogger;
        private EmailFacade emailFacade;
        private NotificacaoService notificacaoService;

        private PedidoService service;

        @BeforeEach
        void setup() {
                pedidoRepository = mock(PedidoRepository.class);
                clienteRepository = mock(ClienteRepository.class);
                appLogger = mock(AppLogger.class);
                emailFacade = mock(EmailFacade.class);
                notificacaoService = mock(NotificacaoService.class);

                service = new PedidoService(
                                pedidoRepository,
                                clienteRepository,
                                appLogger,
                                emailFacade,
                                notificacaoService);
        }

        // =========================================
        // GERAR CÓDIGO
        // =========================================
        @Test
        void gerarCodigoPedido_deveGerarCodigoValido() {
                when(pedidoRepository.existsByCodigoPedido(anyString()))
                                .thenReturn(false);

                String codigo = service.gerarCodigoPedido();

                assertNotNull(codigo);
                assertTrue(codigo.startsWith("BIB-"));
        }

        // =========================================
        // REGISTRAR PEDIDO
        // =========================================
        @Test
        void registrarPedido_deveSalvarPedido() {

                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("teste@email.com");

                Livro livro = new Livro();
                livro.setId(10L);
                livro.setTitulo("Livro Teste");
                livro.setAutor("Autor");
                livro.setIsbn("123");
                livro.setPrecoAprovado(50.0);

                Pedido pedidoSalvo = Pedido.builder().id(99L).build();

                when(pedidoRepository.save(any(Pedido.class)))
                                .thenReturn(pedidoSalvo);

                Pedido resultado = service.registrarPedido(cliente, livro, "COD123");

                assertNotNull(resultado);
                assertEquals(99L, resultado.getId());

                verify(pedidoRepository).save(any(Pedido.class));
                verify(appLogger).success(any(), any(), any(), any());
        }

        // =========================================
        // ATUALIZAR STATUS - SUCESSO
        // =========================================
        @Test
        void atualizarStatus_deveAtualizarCorretamente() {

                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("email@email.com");

                Pedido pedido = Pedido.builder()
                                .id(1L)
                                .comprador(cliente)
                                .statusEnvio(StatusEnvio.AGUARDANDO_ENVIO)
                                .precoLivro(100.0)
                                .build();

                when(pedidoRepository.findById(1L))
                                .thenReturn(Optional.of(pedido));

                when(pedidoRepository.save(any()))
                                .thenAnswer(inv -> inv.getArgument(0));

                Pedido resultado = service.atualizarStatus(
                                1L,
                                StatusEnvio.EM_TRANSITO,
                                "TRACK123");

                assertEquals(StatusEnvio.EM_TRANSITO, resultado.getStatusEnvio());

                verify(pedidoRepository).save(pedido);
                verify(appLogger).success(any(), any(), any(), any());
        }

        // =========================================
        // ATUALIZAR STATUS - NÃO ENCONTRADO
        // =========================================
        @Test
        void atualizarStatus_quandoNaoExiste_deveLancarErro() {

                when(pedidoRepository.findById(1L))
                                .thenReturn(Optional.empty());

                assertThrows(RuntimeException.class, () -> {
                        service.atualizarStatus(1L, StatusEnvio.EM_TRANSITO, null);
                });
        }

        // =========================================
        // CANCELAMENTO (ESTORNO)
        // =========================================
        @Test
        void atualizarStatus_cancelado_deveFazerEstorno() {
                // Arrange
                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("email@email.com");
                cliente.setSaldoTokens(50.0);

                Pedido pedido = Pedido.builder()
                                .id(1L)
                                .comprador(cliente)
                                .statusEnvio(StatusEnvio.AGUARDANDO_ENVIO)
                                .precoLivro(30.0)
                                .build();

                when(pedidoRepository.findById(1L))
                                .thenReturn(Optional.of(pedido));

                when(pedidoRepository.save(any()))
                                .thenAnswer(inv -> inv.getArgument(0));

                when(notificacaoService.criarNotificacaoDashboard(any(), any(), any()))
                                .thenAnswer(inv -> NotificacaoDashboard.builder()
                                                .cliente(inv.getArgument(0))
                                                .mensagem(inv.getArgument(1))
                                                .link(inv.getArgument(2))
                                                .build());

                Pedido resultado = service.atualizarStatus(
                                1L,
                                StatusEnvio.CANCELADO,
                                null);

                assertEquals(StatusEnvio.CANCELADO, resultado.getStatusEnvio());
                assertEquals(80.0, cliente.getSaldoTokens()); // 50.0 + 30.0

                verify(clienteRepository).save(cliente);

                verify(emailFacade).sendHtmlSafe(
                                eq("email@email.com"),
                                eq("Estorno confirmado"),
                                any());

                verify(emailFacade).sendHtmlSafe(
                                eq("email@email.com"),
                                eq("Pedido atualizado"),
                                any());

                verify(notificacaoService, times(2)).criarNotificacaoDashboard(
                                eq(cliente),
                                any(),
                                any());
        }
}