package umc.exs.service.unitary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import umc.exs.mappers.LivroMapper;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.bussiness.GoogleBooksService;
import umc.exs.service.core.bussiness.LivroCompraService;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.cupom.CupomService;
import umc.exs.service.email.EmailService;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

class LivroCompraServiceTest {

        private LivroRepository livroRepository;
        private ClienteRepository clienteRepository;
        private PedidoService pedidoService;
        private EmailService emailService;
        private CupomService cupomService;
        private GamificacaoService gamificacaoService;
        private LogAuditoriaService logAuditoriaService;
        private NotificacaoService notificacaoService;
        private GoogleBooksService googleBooksService;
        private LivroMapper livroMapper;

        private LivroCompraService service;

        @BeforeEach
        void setUp() {
                livroRepository = mock(LivroRepository.class);
                clienteRepository = mock(ClienteRepository.class);
                pedidoService = mock(PedidoService.class);
                emailService = mock(EmailService.class);
                cupomService = mock(CupomService.class);
                gamificacaoService = mock(GamificacaoService.class);
                logAuditoriaService = mock(LogAuditoriaService.class);
                notificacaoService = mock(NotificacaoService.class);

                service = new LivroCompraService(
                                livroRepository,
                                clienteRepository,
                                emailService,
                                pedidoService,
                                cupomService,
                                gamificacaoService,
                                logAuditoriaService,
                                notificacaoService);
        }

        // =========================
        // FACTORY SIMPLES (LOCAL)
        // =========================

        private Obra criarObra() {
                return Obra.builder()
                                .titulo("Livro Teste")
                                .autor("Autor Teste")
                                .build();
        }

        private Livro criarLivro(Long id, double preco) {
                return Livro.builder()
                                .id(id)
                                .titulo("Livro Teste")
                                .autor("Autor Teste")
                                .aprovado(true)
                                .precoAprovado(preco)
                                .obra(criarObra())
                                .build();
        }

        private Cliente criarCliente(String email, double saldo) {
                Cliente c = new Cliente();
                c.setId(1L);
                c.setEmail(email);
                c.setSaldoTokens(saldo);
                // Endereço obrigatório para realizar compra
                Endereco end = new Endereco();
                end.setId(1L);
                end.setCep("01001000");
                end.setRua("Rua Teste");
                end.setNumero("1");
                c.getEnderecos().add(end);
                return c;
        }

        // =========================
        // TESTE 1 - SUCESSO
        // =========================

        @Test
        void realizarCompra_Sucesso_DeletaLivroDeduzSaldo() {

                Long livroId = 1L;
                String email = "buyer@test.com";

                Livro livro = criarLivro(livroId, 10.0);
                Cliente buyer = criarCliente(email, 20.0);

                when(livroRepository.findByIdAndAprovadoTrueWithLock(livroId))
                                .thenReturn(Optional.of(livro));

                when(clienteRepository.findByEmail(email))
                                .thenReturn(Optional.of(buyer));

                when(pedidoService.registrarPedido(any(), any()))
                                .thenReturn(null);

                service.realizarCompra(livroId, email);

                assertEquals(10.0, buyer.getSaldoTokens());

                verify(livroRepository).save(livro);                
                verify(clienteRepository, never()).save(any());
                verify(pedidoService).registrarPedido(buyer, livro);
                verify(emailService).enviar(
                                buyer.getEmail(),
                                "Compra realizada",
                                "Seu livro foi comprado com sucesso");
                verify(gamificacaoService).xpCompra(buyer.getId());
        }

        // =========================
        // TESTE 2 - SALDO INSUFICIENTE
        // =========================

        @Test
        void realizarCompra_SaldoInsuficiente_ThrowsException() {

                Long livroId = 1L;
                String email = "buyer@test.com";

                Livro livro = criarLivro(livroId, 100.0);
                Cliente buyer = criarCliente(email, 50.0);

                when(livroRepository.findByIdAndAprovadoTrueWithLock(anyLong()))
                                .thenReturn(Optional.of(livro));

                when(clienteRepository.findByEmail(anyString()))
                                .thenReturn(Optional.of(buyer));

                RuntimeException ex = assertThrows(
                                RuntimeException.class,
                                () -> service.realizarCompra(livroId, email));

                assertEquals("Saldo insuficiente para completar a compra.", ex.getMessage());

                 verify(livroRepository, never()).save(any());
        }
}