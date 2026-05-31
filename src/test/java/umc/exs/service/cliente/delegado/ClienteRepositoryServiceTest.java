package umc.exs.service.cliente.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;
import umc.exs.service.cliente.EnderecoService;

@ExtendWith(MockitoExtension.class)
class ClienteRepositoryServiceTest {

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    RecuperacaoSenhaRepository tokenRepository;

    @Mock
    EnderecoService enderecoService;

    @InjectMocks
    ClienteRepositoryService service;

    @Test
    void buscarPorId_quandoExiste_retornaCliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        Cliente result = service.buscarPorId(1L);

        assertSame(cliente, result);
    }

    @Test
    void buscarPorId_quandoNaoExiste_lanca() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(1L));
    }

    @Test
    void deletarPorId_aplicaSoftDelete() {
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setAtivo(true);
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.deletarPorId(2L);

        assertFalse(cliente.isAtivo());
        assertNotNull(cliente.getDeletedAt());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void adicionarEnderecoParaUsuarioLogado_seNaoTemEnderecoSelecionado_deveSelecionarOPrimeiro() {
        Endereco endereco = new Endereco();
        endereco.setId(123L);

        Cliente cliente = new Cliente();
        cliente.setEmail("user@test.com");
        cliente.setEnderecos(Set.of(endereco));

        when(clienteRepository.findByEmail("user@test.com")).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(enderecoService).vincularNovoEndereco(any(Cliente.class), any(Endereco.class));

        service.adicionarEnderecoParaUsuarioLogado("user@test.com", new Endereco());

        assertNotNull(cliente.getEnderecoSelecionadoId());
        verify(clienteRepository, atLeastOnce()).save(cliente);
    }

    @Test
    void excluirTokenRecuperacao_deveRemoverRegistro() {
        RecuperacaoSenha registro = new RecuperacaoSenha();
        service.excluirTokenRecuperacao(registro);
        verify(tokenRepository).delete(registro);
    }
}
