package umc.exs.controller_api.unitary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import umc.exs.controller.api.interaction.LivroControllerApi;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.service.core.livros.LivroService;
import umc.exs.dto.mapper.LivroMapper;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.request.compra.LoteRequest;
import umc.exs.dto.request.livro.LivroRequest;

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

    @Test
    void listarTodos_ComPromocao_RetornaListaPromocoes() {
        // Arrange
        when(livroService.listarPromocoesAtivas()).thenReturn(List.of());

        // Act
        ResponseEntity<?> response = controller.listarTodos(true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(livroService).listarPromocoesAtivas();
    }

    @Test
    void listarTodos_SemPromocao_RetornaListaNormal() {
        // Arrange
        when(livroService.listarLivrosAprovados()).thenReturn(List.of());

        // Act
        ResponseEntity<?> response = controller.listarTodos(false);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(livroService).listarLivrosAprovados();
    }

    @Test
    void buscarPorId_ComSucesso() {
        // Arrange
        Long id = 1L;
        when(livroService.buscarPorIdAtivo(id)).thenReturn(mock());

        // Act
        ResponseEntity<?> response = controller.buscarPorId(id);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(livroService).buscarPorIdAtivo(id);
    }

    @Test
    void listarVitrine_ComPromocao() {
        // Arrange
        // Atualizado: Adicionados os matchers any() para os novos parâmetros (estados,
        // generos) no stubbing
        when(livroService.listarPromocoesAtivasPaginado(any(), any(), any(), any()))
                .thenReturn(Page.empty());

        // Act
        // Atualizado: Adicionados os argumentos extras exigidos pelo método
        // (estados=null, generos=null, ordem="relevancia")
        ResponseEntity<?> response = controller.listarVitrine(0, 20, true, null, null, null, "relevancia");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(livroService).listarPromocoesAtivasPaginado(any(), any(), any(), any());
    }

    @Test
    void listarVitrine_SemPromocao() {
        // Arrange
        // Atualizado: Adicionados os matchers any() para os novos parâmetros (estados,
        // generos) no stubbing
        when(livroService.listarLivrosAprovadosPaginado(any(), any(), any(), any()))
                .thenReturn(Page.empty());

        // Act
        // Atualizado: Adicionados os argumentos extras exigidos pelo método
        // (estados=null, generos=null, ordem="relevancia")
        ResponseEntity<?> response = controller.listarVitrine(0, 20, false, null, null, null, "relevancia");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(livroService).listarLivrosAprovadosPaginado(any(), any(), any(), any());
    }

    @Test
    void criarAnuncio_SemAuth_Retorna401() {
        ResponseEntity<?> response = controller.criarAnuncio(null, null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void criarAnuncio_FotoVazia_RetornaBadRequest() {
        MultipartFile foto = mock(MultipartFile.class);
        when(foto.isEmpty()).thenReturn(true);

        ResponseEntity<?> response = controller.criarAnuncio(mockUser, null, foto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void criarAnuncio_FormatoInvalido_RetornaBadRequest() {
        MultipartFile foto = mock(MultipartFile.class);

        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(1000L);
        when(foto.getContentType()).thenReturn("application/pdf");

        ResponseEntity<?> response = controller.criarAnuncio(mockUser, null, foto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void criarAnuncio_Sucesso() throws Exception {
        MultipartFile foto = mock(MultipartFile.class);

        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(1000L);
        when(foto.getContentType()).thenReturn("image/jpeg");
        when(foto.getOriginalFilename()).thenReturn("teste.jpg");
        when(foto.getBytes()).thenReturn(new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0
        });

        LivroRequest dto = new LivroRequest();
        dto.setTitulo("Teste");
        dto.setAutor("Autor Teste");
        dto.setIsbn("1234567890");

        when(livroService.cadastrarVenda(anyString(), any(), any()))
                .thenReturn(mock());

        ResponseEntity<?> response = controller.criarAnuncio(mockUser, dto, foto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void criarLoteVenda_SemAuth_Retorna401() {
        ResponseEntity<?> response = controller.criarLoteVenda(null, null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void criarLoteVenda_SemFotos_RetornaBadRequest() {
        ResponseEntity<?> response = controller.criarLoteVenda(mockUser, null, List.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void criarLoteVenda_FormatoInvalido_RetornaBadRequest() {
        MultipartFile foto = mock(MultipartFile.class);

        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(1000L);
        when(foto.getContentType()).thenReturn("application/pdf");

        ResponseEntity<?> response = controller.criarLoteVenda(mockUser, null, List.of(foto));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void criarLoteVenda_Sucesso() throws Exception {
        MultipartFile foto = mock(MultipartFile.class);

        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(1000L);
        when(foto.getContentType()).thenReturn("image/jpeg");

        when(foto.getOriginalFilename()).thenReturn("teste.jpg");
        when(foto.getBytes()).thenReturn(new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0
        });

        LoteRequest lote = new LoteRequest();
        // preencha campos obrigatórios do lote aqui

        when(livroService.criarLote(anyString(), any(), any()))
                .thenReturn(mock());

        ResponseEntity<?> response = controller.criarLoteVenda(mockUser, lote, List.of(foto));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void cadastrarPorIsbn_SemAuth_Retorna401() {
        ResponseEntity<?> response = controller.cadastrarPorIsbn(null, "123");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void cadastrarPorIsbn_Sucesso() {
        when(livroService.cadastrarPorIsbn(anyString())).thenReturn(mock());

        ResponseEntity<?> response = controller.cadastrarPorIsbn(mockUser, "123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void cadastrarPorIsbn_NaoEncontrado() {
        when(livroService.cadastrarPorIsbn(anyString()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Não encontrado"));

        ResponseEntity<?> response = controller.cadastrarPorIsbn(mockUser, "123");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void listarTodos_emPromocao_true() {
        when(livroService.listarPromocoesAtivas()).thenReturn(List.of());

        ResponseEntity<?> resp = controller.listarTodos(true);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void listarTodos_emPromocao_false() {
        when(livroService.listarLivrosAprovados()).thenReturn(List.of());

        ResponseEntity<?> resp = controller.listarTodos(false);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void listarVitrine_promocao() {
        // Arrange
        Page page = mock(Page.class);
        // Corrigido: Adicionado os 4 matchers exigidos pela nova assinatura do Service
        when(livroService.listarPromocoesAtivasPaginado(any(), any(), any(), any()))
                .thenReturn(page);
        when(page.map(any())).thenReturn(page);

        // Act
        // Corrigido: Adicionado os parâmetros que faltavam (estados=null, generos=null,
        // ordem="relevancia")
        ResponseEntity<?> resp = controller.listarVitrine(0, 10, true, "java", null, null, "relevancia");

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void listarVitrine_normal() {
        // Arrange
        Page page = mock(Page.class);
        // Corrigido: Adicionado os 4 matchers exigidos pela nova assinatura do Service
        when(livroService.listarLivrosAprovadosPaginado(any(), any(), any(), any()))
                .thenReturn(page);
        when(page.map(any())).thenReturn(page);

        // Act
        // Corrigido: Adicionado os parâmetros que faltavam (estados=null, generos=null,
        // ordem="relevancia")
        ResponseEntity<?> resp = controller.listarVitrine(0, 10, false, null, null, null, "relevancia");

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void criarLote_semUsuario() {
        ResponseEntity<?> resp = controller.criarLoteVenda(null, new LoteRequest(), List.of());

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void criarLote_semFotos() {
        UserDetails user = mock(UserDetails.class);

        ResponseEntity<?> resp = controller.criarLoteVenda(user, new LoteRequest(), List.of());

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void criarLote_arquivoGrande() {
        UserDetails user = mock(UserDetails.class);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getSize()).thenReturn(11 * 1024 * 1024L);
        when(file.getOriginalFilename()).thenReturn("img.jpg");

        ResponseEntity<?> resp = controller.criarLoteVenda(user, new LoteRequest(), List.of(file));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void criarLote_tipoInvalido() {
        UserDetails user = mock(UserDetails.class);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("file.pdf");

        ResponseEntity<?> resp = controller.criarLoteVenda(user, new LoteRequest(), List.of(file));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void criarLote_imagemInvalida() throws Exception {
        UserDetails user = mock(UserDetails.class);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(new byte[] { 0x00, 0x00, 0x00 });
        when(file.getOriginalFilename()).thenReturn("img.png");

        ResponseEntity<?> resp = controller.criarLoteVenda(user, new LoteRequest(), List.of(file));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void criarLote_erroLeituraArquivo() throws Exception {
        UserDetails user = mock(UserDetails.class);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenThrow(new RuntimeException("falha"));
        when(file.getOriginalFilename()).thenReturn("img.png");

        ResponseEntity<?> resp = controller.criarLoteVenda(user, new LoteRequest(), List.of(file));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void criarLote_erroService() throws IOException {
        UserDetails user = mock(UserDetails.class);
        MultipartFile file = mock(MultipartFile.class);

        when(user.getUsername()).thenReturn("user");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("img.png");
        when(file.getBytes()).thenReturn(new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 }); // PNG válido

        when(livroService.criarLote(any(), any(), any()))
                .thenThrow(new RuntimeException("erro"));

        ResponseEntity<?> resp = controller.criarLoteVenda(user, new LoteRequest(), List.of(file));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void criarAnuncio_semUsuario() {
        ResponseEntity<?> resp = controller.criarAnuncio(null, new LivroRequest(), null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void criarAnuncio_semFoto() {
        UserDetails user = mock(UserDetails.class);

        ResponseEntity<?> resp = controller.criarAnuncio(user, new LivroRequest(), null);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void comprarLivro_semUsuario() {
        ResponseEntity<?> resp = controller.comprarLivro(1L, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void comprarLivro_runtimeException() {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("user");

        doThrow(new RuntimeException("erro"))
                .when(livroService).realizarCompra(1L, "user");

        ResponseEntity<?> resp = controller.comprarLivro(1L, user);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void comprarLivro_exceptionGenerica() {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("user");

        doThrow(new RuntimeException())
                .when(livroService).realizarCompra(any(), any());

        ResponseEntity<?> resp = controller.comprarLivro(1L, user);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void comprarCarrinho_semUsuario() {
        ResponseEntity<?> resp = controller.comprarCarrinho(null, new CarrinhoCompraRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void comprarCarrinho_erro() {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("user");

        when(livroService.comprarCarrinho(any(), any()))
                .thenThrow(new RuntimeException("erro"));

        ResponseEntity<?> resp = controller.comprarCarrinho(user, new CarrinhoCompraRequest());

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void cadastrarIsbn_semUsuario() {
        ResponseEntity<?> resp = controller.cadastrarPorIsbn(null, "123");

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void cadastrarIsbn_notFound() {
        UserDetails user = mock(UserDetails.class);

        when(livroService.cadastrarPorIsbn("123"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("não achou"));

        ResponseEntity<?> resp = controller.cadastrarPorIsbn(user, "123");

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

}
