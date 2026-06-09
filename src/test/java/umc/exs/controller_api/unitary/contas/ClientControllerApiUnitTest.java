package umc.exs.controller_api.unitary.contas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import umc.exs.controller.api.contas.ClientControllerApi;
import umc.exs.dto.mapper.ClienteMapper;
import umc.exs.dto.response.admin.VendaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.livros.MinhasVendasService;

class ClientControllerApiUnitTest {

    private ClienteService clienteService;
    private MinhasVendasService minhasVendasService;
    private ClienteMapper clienteMapper;
    private ClientControllerApi controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        clienteService = mock(ClienteService.class);
        minhasVendasService = mock(MinhasVendasService.class);
        clienteMapper = mock(ClienteMapper.class);

        controller = new ClientControllerApi(
                clienteService,
                minhasVendasService,
                clienteMapper);

        user = User.withUsername("cliente@email.com")
                .password("pass")
                .authorities(List.of(() -> "USER"))
                .build();
    }

    @Test
    void getPerfilJson_SemClienteRetorna404() {
        when(clienteService.buscarClientePorEmail(user.getUsername()))
                .thenReturn(Optional.empty());

        ResponseEntity<ClientePerfilResponse> resp = controller.getPerfilJson(user);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(clienteService).buscarClientePorEmail(user.getUsername());
    }

    @Test
    void perfilJson_SemAuthRetorna401() {
        ResponseEntity<ClientePerfilResponse> resp = controller.perfilJson(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(clienteService, minhasVendasService, clienteMapper);
    }

    @Test
    void verificarEmail_RetornaPresente() {
        when(clienteService.buscarClientePorEmail("email@mail.com"))
                .thenReturn(Optional.of(mock(Cliente.class)));

        ResponseEntity<Boolean> resp = controller.verificarEmail("email@mail.com");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody());
    }

    @Test
    void removerEndereco_DisparaRedirecionamentoMesmoComExcecao() {
        Principal principal = () -> "cliente@email.com";
        RedirectAttributes redirect = mock(RedirectAttributes.class);

        when(clienteService.buscarClientePorEmail(principal.getName()))
                .thenThrow(new RuntimeException("falha"));

        String view = controller.removerEndereco(1L, principal, redirect);

        assertEquals("redirect:/clientes/meu-perfil", view);
        verify(redirect).addFlashAttribute(eq("erro"), contains("Erro ao remover endereço"));
    }

    @Test
    void listarMinhasVendas_SemAuthRetorna401() {
        ResponseEntity<List<VendaResponse.Resumo>> resp = controller.listarMinhasVendas(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getEnderecoSelecionado_RetornaPrimeiroEnderecoQuandoNenhumSelecionado() {
        Endereco e1 = new Endereco();
        e1.setId(10L);
        Cliente cliente = mock(Cliente.class);
        when(cliente.getEnderecos()).thenReturn(Set.of(e1));

        when(cliente.getEnderecoSelecionadoId()).thenReturn(null);

        when(clienteService.buscarClientePorEmail(user.getUsername()))
                .thenReturn(Optional.of(cliente));

        ResponseEntity<Endereco> resp = controller.getEnderecoSelecionado(user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(10L, resp.getBody().getId());

    }

    @Test
    void getHistorico_RetornaOk() {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getId()).thenReturn(77L);
        when(clienteService.buscarClientePorEmail(user.getUsername()))
                .thenReturn(Optional.of(cliente));

        List<Transacao> txs = List.of(mock(Transacao.class));
        when(clienteService.listarHistoricoTransacoes(77L)).thenReturn(txs);

        ResponseEntity<List<Transacao>> resp = controller.getHistorico(user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(txs, resp.getBody());
    }
}
