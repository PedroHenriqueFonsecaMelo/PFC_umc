package umc.exs.controller_api.unitary.compras;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.mapstruct.factory.Mappers;

import umc.exs.controller.api.compras.ListaDesejosController;
import umc.exs.dto.mapper.ListaDesejosMapper;
import umc.exs.dto.response.cliente.ListaDesejosResponse;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.foundation.ListaDesejos;
import umc.exs.service.core.dashboard.ListaDesejosService;

class ListaDesejosControllerUnitTest {

    private ListaDesejosService service;
    private ListaDesejosMapper mapper;
    private ListaDesejosController controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        service = mock(ListaDesejosService.class);
        mapper = Mappers.getMapper(ListaDesejosMapper.class);
        controller = new ListaDesejosController(service, mapper);

        user = User.withUsername("cliente@email.com")
                .password("pass")
                .authorities("USER")
                .build();
    }

    @Test
    void listar_SemAuth_Retorna401() {
        ResponseEntity<List<ListaDesejosResponse>> resp = controller.listar(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void adicionar_ComSucesso_Retorna201() {
        Map<String, String> dto = Map.of("isbn", "123");

        // Como o service retorna entidade (ListaDesejos), mockamos a entidade.
        Cliente cliente = mock(Cliente.class);
        ListaDesejos entity = mock(ListaDesejos.class);

        when(entity.getId()).thenReturn(1L);
        when(entity.getIsbn()).thenReturn("123");
        when(entity.isPreReservaAtiva()).thenReturn(false);
        when(entity.getCliente()).thenReturn(cliente);
        when(cliente.getNome()).thenReturn("Cliente");

        when(service.adicionarDesejo(eq(user.getUsername()), eq("123"), ""))
                .thenReturn(entity);

        ResponseEntity<ListaDesejosResponse> resp = controller.adicionar(user, dto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        verify(service).adicionarDesejo(user.getUsername(), "123", "");
    }
}

