package umc.exs.service.cliente.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import umc.exs.dto.mapper.ClienteMapper;
import umc.exs.dto.request.cliente.ClienteUpdateRequest;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.service.cliente.EnderecoService;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class ClientePerfilServiceTest {

    @Mock
    ClienteRepositoryService repositoryService;

    @Mock
    EnderecoService enderecoService;

    @Mock
    ClienteMapper clienteMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    LogAuditoriaService auditoria;

    @InjectMocks
    ClientePerfilService service;

    @Test
    void atualizarDados_deveAtualizarCamposESalvar() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("cliente@test.com");

        ClienteUpdateRequest dto = new ClienteUpdateRequest();
        dto.setNome("Nome Atualizado");
        dto.setDatanasc(LocalDate.of(1990, 1, 1));
        dto.setSenha("NovaSenha");

        when(repositoryService.buscarPorId(1L)).thenReturn(cliente);
        when(passwordEncoder.encode("NovaSenha")).thenReturn("hashed");
        when(repositoryService.salvar(cliente)).thenReturn(cliente);

        Cliente salvo = service.atualizarDados(1L, dto);

        assertEquals("Nome Atualizado", salvo.getNome());
        assertEquals("hashed", salvo.getSenha());
        
        // CORREÇÃO: Alterado de 'null' para 'any()' para aceitar tanto uma lista vazia quanto null
        verify(enderecoService).sincronizarEnderecos(eq(cliente), any());
        verify(auditoria).registrarLog(eq("CLIENTE_DADOS_ATUALIZADOS"), eq(1L), eq("cliente@test.com"), anyString());
    }

    @Test
    void cadastrarCompleto_deveSalvarClienteComEndereco() {
        SignupRequest req = new SignupRequest();
        req.setNome("Cliente");
        req.setSenha("senha123");
        req.setEmail("cliente@test.com");

        Cliente cliente = new Cliente();
        cliente.setEmail("cliente@test.com");

        Endereco enderecoDTO = new Endereco();
        Endereco endereco = new Endereco();

        when(clienteMapper.paraEntidade(req)).thenReturn(cliente);
        when(passwordEncoder.encode("senha123")).thenReturn("hash");
        when(enderecoService.saveOrReuseEndereco(enderecoDTO)).thenReturn(endereco);
        when(repositoryService.salvar(cliente)).thenReturn(cliente);

        Cliente result = service.cadastrarCompleto(req, enderecoDTO);

        assertSame(cliente, result);
        verify(repositoryService).salvar(cliente);
        assertEquals(1, cliente.getEnderecos().size());
    }
}