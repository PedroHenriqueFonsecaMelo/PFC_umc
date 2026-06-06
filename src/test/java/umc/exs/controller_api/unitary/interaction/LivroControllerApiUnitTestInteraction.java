package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import umc.exs.controller.api.interaction.LivroControllerApi;
import umc.exs.dto.mapper.LivroMapper;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.service.core.livros.LivroService;

class LivroControllerApiUnitTestInteraction {

    private LivroService livroService;
    private LivroMapper livroMapper;
    private LivroControllerApi controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        livroService = mock(LivroService.class);
        livroMapper = Mappers.getMapper(LivroMapper.class);
        controller = new LivroControllerApi(livroService, livroMapper);

        user = User.withUsername("test@example.com")
                .password("pass")
                .authorities("USER")
                .build();
    }

    @Test
    void criarLoteVenda_SemAuth_Retorna401() {
        ResponseEntity<Object> resp = (ResponseEntity<Object>) controller.criarLoteVenda(null, null, List.of());

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Usuário precisa estar logado.", resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void comprarLivro_SemAuth_Retorna401() {
        ResponseEntity<Object> resp = (ResponseEntity<Object>) controller.comprarLivro(1L, null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Usuário precisa estar logado.", resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void comprarLivro_ComSucesso_RetornaOk() {
        doNothing().when(livroService).realizarCompra(eq(1L), eq(user.getUsername()));

        ResponseEntity<Object> resp = (ResponseEntity<Object>) controller.comprarLivro(1L, user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Compra realizada com sucesso! Tokens transferidos.", resp.getBody());
        verify(livroService).realizarCompra(eq(1L), eq(user.getUsername()));
    }

    @Test
    void comprarCarrinho_ComSucesso_RetornaOk() {
        CarrinhoCompraRequest req = new CarrinhoCompraRequest();
        req.setLivroIds(List.of(1L, 2L));

        CarrinhoCompraResponse mockResponse = CarrinhoCompraResponse.builder()
                .totalSolicitados(2)
                .totalComprados(2)
                .totalGasto(25.0)
                .saldoRestante(75.0)
                .build();

        when(livroService.comprarCarrinho(eq(user.getUsername()), eq(req)))
                .thenReturn(mockResponse);

        ResponseEntity<Object> resp = (ResponseEntity<Object>) controller.comprarCarrinho(user, req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(mockResponse, resp.getBody());
        verify(livroService).comprarCarrinho(eq(user.getUsername()), eq(req));
    }

    @Test
    void comprarCarrinho_SemAuth_Retorna401() {
        CarrinhoCompraRequest req = new CarrinhoCompraRequest();
        req.setLivroIds(List.of(1L));

        ResponseEntity<Object> resp = (ResponseEntity<Object>) controller.comprarCarrinho(null, req);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Usuário precisa estar logado.", resp.getBody());
        verifyNoInteractions(livroService);
    }
}

