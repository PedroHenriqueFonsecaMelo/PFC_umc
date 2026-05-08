package umc.exs.service.unitary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.bussiness.LivroCompraService;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.email.EmailService;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;

class LivroCompraServiceTest {

    private LivroRepository livroRepository;
    private ClienteRepository clienteRepository;
    private PedidoService pedidoService;
    private EmailService emailService;
    private GamificacaoService gamificacaoService;
    private LogAuditoriaService logAuditoriaService;
    private LivroCompraService service;

    @BeforeEach
    void setUp() {
        livroRepository = mock(LivroRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        pedidoService = mock(PedidoService.class);
        emailService = mock(EmailService.class);
        gamificacaoService = mock(GamificacaoService.class);
        logAuditoriaService = mock(LogAuditoriaService.class);
        
        service = new LivroCompraService(
            livroRepository, 
            clienteRepository, 
            emailService, 
            pedidoService, 
            gamificacaoService, 
            logAuditoriaService
        );
    }

    @SuppressWarnings("null")
    @Test
    void realizarCompra_Sucesso_DeletaLivroDeduzSaldo() {
        // Arrange
        Long livroId = 1L;
        String email = "buyer@test.com";
        
        Livro livro = Livro.builder()
                .id(livroId)
                .aprovado(true)
                .precoAprovado(10.0)
                .titulo("Test Book")
                .build();
                
        Cliente buyer = new Cliente();
        buyer.setId(1L);
        buyer.setEmail(email);
        buyer.setSaldoTokens(20.0);

        // CORREÇÃO 1: Nome do método deve ser exatamente o que o Service chama
        when(livroRepository.findByIdAndAprovadoTrueWithLock(livroId))
                .thenReturn(Optional.of(livro));
        
        when(clienteRepository.findByEmail(email))
                .thenReturn(Optional.of(buyer));
        
        // CORREÇÃO 2: Evitar o erro "Only void methods can doNothing()"
        // Se registrarPedido retornar algo, usamos thenReturn. 
        // Se não soubermos o retorno, thenReturn(null) resolve para objetos.
        when(pedidoService.registrarPedido(any(), any())).thenReturn(null);

        // Act
        service.realizarCompra(livroId, email);

        // Assert
        assertEquals(10.0, buyer.getSaldoTokens());
        verify(livroRepository).delete(livro);
        verify(clienteRepository).save(buyer);
        verify(pedidoService).registrarPedido(buyer, livro);
        verify(emailService).enviar(eq(email), anyString(), anyString());
        verify(gamificacaoService).xpCompra(buyer.getId());
    }

    @SuppressWarnings("null")
    @Test
    void realizarCompra_SaldoInsuficiente_ThrowsException() {
        // Arrange
        Long livroId = 1L;
        String email = "buyer@test.com";
        
        Livro livro = Livro.builder()
                .id(livroId)
                .aprovado(true)
                .precoAprovado(100.0) // Caro demais
                .build();
                
        Cliente buyer = new Cliente();
        buyer.setSaldoTokens(50.0); // Saldo insuficiente

        when(livroRepository.findByIdAndAprovadoTrueWithLock(anyLong()))
                .thenReturn(Optional.of(livro));
        when(clienteRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(buyer));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            service.realizarCompra(livroId, email)
        );
        
        assertEquals("Saldo insuficiente", ex.getMessage());
        verify(livroRepository, never()).delete(any());
    }
}