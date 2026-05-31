package umc.exs.service.cliente;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.usuario.EnderecoRepository;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {

    @Mock
    private EnderecoRepository enderecoRepository;

    @InjectMocks
    private EnderecoService service;

    @Test
    void saveOrReuseEndereco_quandoEnderecoExistente_retornaExistente() {
        Endereco dto = new Endereco();
        dto.setCep("01000-000");
        dto.setRua("Rua A");
        dto.setNumero("1");

        Endereco existente = new Endereco();
        existente.setId(10L);

        when(enderecoRepository.findByValueFields(
                eq(dto.getCep()),
                eq(dto.getRua()),
                eq(dto.getNumero()),
                eq(dto.getComplemento()),
                eq(dto.getBairro()),
                eq(dto.getCidade()),
                eq(dto.getEstado())))
                .thenReturn(Optional.of(existente));

        Endereco res = service.saveOrReuseEndereco(dto);
        assertSame(existente, res);
        verify(enderecoRepository, never()).save(any(Endereco.class));
    }

    @Test
    void saveOrReuseEndereco_quandoNaoExiste_salvaRetornaSalvo() {
        Endereco dto = new Endereco();
        dto.setCep("01000-000");
        dto.setRua("Rua A");
        dto.setNumero("1");

        when(enderecoRepository.findByValueFields(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        Endereco salvo = new Endereco();
        salvo.setId(1L);
        when(enderecoRepository.save(dto)).thenReturn(salvo);

        Endereco res = service.saveOrReuseEndereco(dto);
        assertEquals(1L, res.getId());
        verify(enderecoRepository).save(dto);
    }

    @Test
    void vincularNovoEndereco_deveVincularBidirecional() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        Endereco dto = new Endereco();
        dto.setId(2L);

        Endereco enderecoSalvo = new Endereco();
        enderecoSalvo.setId(2L);

        cliente.setEnderecos(new java.util.HashSet<>());
        enderecoSalvo.setClientes(new java.util.HashSet<>());

        when(enderecoRepository.findByValueFields(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(enderecoSalvo));

        service.vincularNovoEndereco(cliente, dto);

        assertTrue(cliente.getEnderecos().contains(enderecoSalvo));
        assertTrue(enderecoSalvo.getClientes().contains(cliente));
    }

    @Test
    void deletarEnderecoDoCliente_quandoEnderecoNaoExiste_lancaEntityNotFound() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        when(enderecoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.deletarEnderecoDoCliente(cliente, 99L));
    }

    @Test
    void deletarEnderecoDoCliente_quandoEnderecoNaoPertence_lancaIllegalArgumentException() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEnderecos(new java.util.HashSet<>());

        Endereco endereco = new Endereco();
        endereco.setId(5L);
        endereco.setClientes(new java.util.HashSet<>());

        when(enderecoRepository.findById(5L)).thenReturn(Optional.of(endereco));

        assertThrows(IllegalArgumentException.class,
                () -> service.deletarEnderecoDoCliente(cliente, 5L));
    }

    @Test
    void sincronizarEnderecos_quandoIdsNulos_retornaSemAlterar() {
        Cliente cliente = new Cliente();
        cliente.setEnderecos(new java.util.HashSet<>());

        service.sincronizarEnderecos(cliente, null);
        assertTrue(cliente.getEnderecos().isEmpty());
        verifyNoInteractions(enderecoRepository);
    }

    @Test
    void sincronizarEnderecos_deveAdicionarNovoEnderecoComVinculo() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEnderecos(new java.util.HashSet<>());

        Endereco dtoNovo = new Endereco();
        dtoNovo.setId(null);
        dtoNovo.setCep("01000-000");

        Endereco enderecoReuso = new Endereco();
        enderecoReuso.setId(2L);
        enderecoReuso.setClientes(new java.util.HashSet<>());

        when(enderecoRepository.findByValueFields(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(enderecoReuso));

        service.sincronizarEnderecos(cliente, List.of(dtoNovo));

        assertEquals(1, cliente.getEnderecos().size());
        Endereco vinculado = cliente.getEnderecos().iterator().next();
        assertEquals(2L, vinculado.getId());
        assertTrue(enderecoReuso.getClientes().contains(cliente));
    }
}
