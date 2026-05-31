package umc.exs.controller_api.unitary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import umc.exs.controller.api.interaction.LivroControllerApi;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.service.core.livros.LivroService;
import umc.exs.dto.mapper.LivroMapper;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;

class LivroControllerApiUnitTest {

    private LivroService livroService;
    private LivroMapper livroMapper;
    private LivroControllerApi controller;

    private UserDetails mockUser;

    @BeforeEach
    void setUp() {
        livroService = mock(LivroService.class);
        livroMapper = Mappers.getMapper(LivroMapper.class);
        controller = new LivroControllerApi(livroService, livroMapper);

        mockUser = User.withUsername("test@example.com")
                .password("pass")
                .authorities("USER")
                .build();
    }

    @Test
    void comprarLivro_ComSucesso_RetornaOk() {
        // Arrange
        Long livroId = 1L;
        doNothing().when(livroService).realizarCompra(eq(livroId), eq("test@example.com"));

        // Act
        ResponseEntity<?> response = controller.comprarLivro(livroId, mockUser);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Compra realizada com sucesso! Tokens transferidos.", response.getBody());
        verify(livroService).realizarCompra(livroId, "test@example.com");
    }

    @Test
    void comprarLivro_SemAuth_Retorna401() {
        // Act
        ResponseEntity<?> response = controller.comprarLivro(1L, null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(livroService);
    }

    @Test
    void comprarLivro_ServiceException_RetornaBadRequest() {
        // Arrange
        Long livroId = 1L;

        doThrow(new IllegalStateException("Saldo insuficiente"))
                .when(livroService)
                .realizarCompra(eq(livroId), eq("test@example.com"));

        // Act
        ResponseEntity<?> response = controller.comprarLivro(livroId, mockUser);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Saldo insuficiente", response.getBody());

        verify(livroService)
                .realizarCompra(livroId, "test@example.com");
    }

    @Test
    void comprarCarrinho_Sucesso_ComMultiplosLivros_RetornaResponseCompleto() {
        // Arrange
        CarrinhoCompraRequest request = new CarrinhoCompraRequest();
        request.setLivroIds(Arrays.asList(1L, 2L));
        CarrinhoCompraResponse mockResponse = CarrinhoCompraResponse.builder()
                .totalSolicitados(2)
                .totalComprados(2)
                .totalGasto(25.0)
                .saldoRestante(75.0)
                .build();
        when(livroService.comprarCarrinho(eq("test@example.com"), eq(request)))
                .thenReturn(mockResponse);

        // Act
        ResponseEntity<?> response = controller.comprarCarrinho(mockUser, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(livroService).comprarCarrinho("test@example.com", request);
    }

    @Test
    void comprarCarrinho_SemAuth_Retorna401() {
        // Arrange
        CarrinhoCompraRequest request = new CarrinhoCompraRequest();
        request.setLivroIds(List.of(1L));

        // Act
        ResponseEntity<?> response = controller.comprarCarrinho(null, request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(livroService);
    }

    @Test
    void comprarCarrinho_ServiceThrowsSaldoInsuficiente_RetornaBadRequest() {
        // Arrange
        CarrinhoCompraRequest request = new CarrinhoCompraRequest();
        request.setLivroIds(List.of(1L));
        doThrow(new RuntimeException("Saldo insuficiente. Necessário: T$ 50.00 | Disponível: T$ 20.00"))
                .when(livroService).comprarCarrinho(anyString(), any(CarrinhoCompraRequest.class));

        // Act
        ResponseEntity<?> response = controller.comprarCarrinho(mockUser, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(((String) response.getBody()).contains("Saldo insuficiente"));
        verify(livroService).comprarCarrinho("test@example.com", request);
    }

    @Test
    void comprarCarrinho_ListaVazia_ThrowsValidationButControllerHandles() {
        // Arrange (validation @Valid catches in real, unit focuses controller)
        CarrinhoCompraRequest request = new CarrinhoCompraRequest();
        request.setLivroIds(List.of()); // empty
        doThrow(new RuntimeException("O carrinho está vazio.")).when(livroService).comprarCarrinho(anyString(),
                any(CarrinhoCompraRequest.class));

        // Act & Assert (controller wraps)
        ResponseEntity<?> response = controller.comprarCarrinho(mockUser, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
