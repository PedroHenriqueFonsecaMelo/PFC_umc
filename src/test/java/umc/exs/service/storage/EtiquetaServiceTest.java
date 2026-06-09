package umc.exs.service.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.negocios.PedidoRepository;

class EtiquetaServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private EtiquetaService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deveGerarEtiquetaComSucesso() {

        // ====== Montando objetos manualmente ======

        Endereco endereco = new Endereco();
        endereco.setId(1L);
        endereco.setRua("Rua A");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("12345678");

        Cliente cliente = new Cliente();
        cliente.setNome("João");
        cliente.setEnderecos(Set.of(endereco));

        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setComprador(cliente);
        pedido.setTituloLivro("Livro Teste");

        // ====== Mock ======

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.of(pedido));

        // ====== Execução ======

        byte[] resultado = service.gerarEtiqueta(1L);

        // ====== Verificações ======

        assertNotNull(resultado);
        assertTrue(resultado.length > 0);

        verify(pedidoRepository).findById(1L);
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoExiste() {

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.gerarEtiqueta(1L);
        });
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoTemEndereco() {

        Cliente cliente = new Cliente();
        cliente.setNome("João");
        cliente.setEnderecos(Set.of()); // vazio

        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setComprador(cliente);

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.of(pedido));

        assertThrows(IllegalStateException.class, () -> {
            service.gerarEtiqueta(1L);
        });
    }
}