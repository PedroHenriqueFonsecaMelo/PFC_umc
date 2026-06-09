package umc.exs.service.core.livros.delegado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        cliente.setEnderecos(Set.of(new Endereco())); // apenas para não ficar vazio

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

        service.realizarCompra(10L, "teste@email.com");

        assertEquals(50.0, cliente.getSaldoTokens());

        verify(pedidoService).registrarPedido(any(), any(), anyString());
        verify(emailFacade, atLeastOnce()).sendHtmlSafe(any(), any(), any());
        verify(logAuditoria).registrarLog(any(), any(), any(), any());
        verify(gamificacaoService).xpCompra(anyLong());
    }

    @Test
    void deveFalharQuandoLivroNaoExiste() {

        when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.realizarCompra(10L, "teste@email.com"));
    }

    @Test
    void deveFalharQuandoClienteNaoExiste() {

        when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                .thenReturn(Optional.of(livro));

        when(clienteRepository.findByEmail("teste@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.realizarCompra(10L, "teste@email.com"));
    }

    @Test
    void deveFalharQuandoSaldoInsuficiente() {

        cliente.setSaldoTokens(10.0);

        when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                .thenReturn(Optional.of(livro));

        when(clienteRepository.findByEmail("teste@email.com"))
                .thenReturn(Optional.of(cliente));

        assertThrows(IllegalStateException.class, () -> service.realizarCompra(10L, "teste@email.com"));
    }

    @Test
    void deveFalharQuandoNaoTemEndereco() {

        cliente.setEnderecos(Set.of());

        when(livroRepository.findByIdAndAprovadoTrueWithLock(10L))
                .thenReturn(Optional.of(livro));

        when(clienteRepository.findByEmail("teste@email.com"))
                .thenReturn(Optional.of(cliente));

        assertThrows(IllegalStateException.class, () -> service.realizarCompra(10L, "teste@email.com"));
    }

}
