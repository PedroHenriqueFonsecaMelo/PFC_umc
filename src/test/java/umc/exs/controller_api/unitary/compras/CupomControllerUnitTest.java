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

import umc.exs.controller.api.compras.CupomController;
import umc.exs.dto.mapper.CupomMapper;
import umc.exs.dto.response.compras.CupomResponse;
import umc.exs.service.cupom.CupomService;

class CupomControllerUnitTest {

    private CupomService service;
    private CupomMapper mapper;
    private CupomController controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        service = mock(CupomService.class);
        mapper = Mappers.getMapper(CupomMapper.class);
        controller = new CupomController(service, mapper);

        user = User.withUsername("cliente@email.com")
                .password("pass")
                .authorities("USER")
                .build();
    }

    @Test
    void meusCupons_SemAuth_Retorna401() {
        ResponseEntity<List<CupomResponse>> resp = controller.meusCupons(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void validar_ComSucesso_Retorna200() {
        when(service.validarCupomParaTotal(eq("CUPOM10"), eq(user.getUsername()), eq(100.0)))
                .thenReturn(Map.of("valido", true, "desconto", 10.0));

        ResponseEntity<Map<String, Object>> resp = controller.validar("CUPOM10", 100.0, user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(true, resp.getBody().get("valido"));
        verify(service).validarCupomParaTotal("CUPOM10", user.getUsername(), 100.0);
    }

    @Test
    void validar_Exception_Retorna400() {
        when(service.validarCupomParaTotal(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("cupom inválido"));

        ResponseEntity<Map<String, Object>> resp = controller.validar("X", 1.0, user);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(false, resp.getBody().get("valido"));
        assertEquals("cupom inválido", resp.getBody().get("mensagem"));
    }
}

