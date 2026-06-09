package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import umc.exs.controller.api.interaction.LivroControllerApi;
import umc.exs.dto.mapper.LivroMapper;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.request.compra.LoteRequest;
import umc.exs.dto.request.livro.LivroRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;

import umc.exs.service.core.livros.LivroService;

class LivroControllerApiUnitTestMissing {

    private LivroService livroService;
    private LivroMapper livroMapper;
    private LivroControllerApi controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        livroService = mock(LivroService.class);
        livroMapper = Mappers.getMapper(LivroMapper.class);
        controller = new LivroControllerApi(livroService, livroMapper);

        user = User.withUsername("test@example.com").password("pass").authorities("USER").build();
    }

    private static MultipartFile multipartImage(String name, String contentType, byte[] bytes, long sizeOverride) {
        // Spring MockMultipartFile usa os bytes informados para tamanho real,
        // então sizeOverride é usado para criar um mock que retorne getSize() maior
        return new MockMultipartFile(name, name + ".bin", contentType, bytes) {
            @Override
            public long getSize() {
                return sizeOverride;
            }
        };
    }

    @Test
    void listarTodos_SemPromocao_DeveRetornar200ComListaVaziaOuMapeada() {
        when(livroService.listarLivrosAprovados()).thenReturn(List.of());

        ResponseEntity<List<?>> resp = (ResponseEntity<List<?>>) (ResponseEntity<?>) controller.listarTodos(false);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
        verify(livroService).listarLivrosAprovados();
    }

    @Test
    void buscarPorId_DeveChamarServiceERetornarMappedResponse() {
        var ativo = mock(umc.exs.model.entidades.livro.Livro.class);

        when(livroService.buscarPorIdAtivo(1L)).thenReturn(ativo);

        ResponseEntity<?> resp = controller.buscarPorId(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(livroService).buscarPorIdAtivo(1L);
        assertNotNull(resp.getBody());
    }

    @Test
    void criarLoteVenda_SemAuth_Retorna401() {
        ResponseEntity<Object> resp = controller.criarLoteVenda(null, mock(LoteRequest.class), List.of());

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Usuário precisa estar logado.", resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarLoteVenda_SemFotos_Retorna400() {
        ResponseEntity<Object> resp = controller.criarLoteVenda(user, mock(LoteRequest.class), List.of());

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("É necessário adicionar pelo menos uma foto do livro.", resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarLoteVenda_TamanhoMaiorQue10MB_Retorna400() {
        LoteRequest req = mock(LoteRequest.class);
        byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
        MultipartFile foto = multipartImage("f1", "image/jpeg", jpegHeader, 11 * 1024 * 1024L);

        ResponseEntity<Object> resp = controller.criarLoteVenda(user, req, List.of(foto));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(
                Map.of("erro", "Arquivo '" + foto.getOriginalFilename() + "' excede o limite de 10 MB."),
                resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarLoteVenda_TipoMimeInvalido_Retorna400() {
        LoteRequest req = mock(LoteRequest.class);
        byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
        MultipartFile foto = multipartImage("f1", "application/pdf", jpegHeader, 1024);

        ResponseEntity<Object> resp = controller.criarLoteVenda(user, req, List.of(foto));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(
                Map.of("erro",
                        "Formato inválido: '" + foto.getOriginalFilename() +
                                "'. Apenas JPEG, PNG e WebP são aceitos."),
                resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarLoteVenda_MagicBytesInvalidos_Retorna400() {
        LoteRequest req = mock(LoteRequest.class);
        byte[] invalid = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        MultipartFile foto = multipartImage("f1", "image/png", invalid, 1024);

        ResponseEntity<Object> resp = controller.criarLoteVenda(user, req, List.of(foto));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(Map.of("erro", "Arquivo '" + foto.getOriginalFilename() + "' não é uma imagem válida."),
                resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarLoteVendaIOExceptionAoLerImagem_Retorna400() throws IOException {
        LoteRequest req = mock(LoteRequest.class);
        MultipartFile foto = new MockMultipartFile("f1", "foto.jpg", "image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 }) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("falha");
            }
        };

        ResponseEntity<Object> resp = controller.criarLoteVenda(user, req, List.of(foto));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(
                Map.of("erro", "Não foi possível verificar o arquivo: " + foto.getOriginalFilename()),
                resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarLoteVenda_ComSucesso_ChamaServiceEVaiParaOk() throws Exception {
        LoteRequest req = mock(LoteRequest.class);
        Lote lote = mock(Lote.class);

        byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
        MultipartFile foto = multipartImage("f1", "image/jpeg", jpegHeader, 1024);

        when(livroService.criarLote(eq(user.getUsername()), eq(req), eq(List.of(foto)))).thenReturn(lote);

        ResponseEntity<Object> resp = controller.criarLoteVenda(user, req, List.of(foto));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(lote, resp.getBody());
        verify(livroService).criarLote(eq(user.getUsername()), eq(req), eq(List.of(foto)));
    }

    @Test
    void criarAnuncio_SemAuth_Retorna401() {
        ResponseEntity<Object> resp = controller.criarAnuncio(null, mock(LivroRequest.class),
                mock(MultipartFile.class));
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Usuário precisa estar logado.", resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarAnuncio_SemFoto_Retorna400() {
        ResponseEntity<Object> resp = controller.criarAnuncio(user, mock(LivroRequest.class), null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(Map.of("erro", "É necessário enviar uma foto do livro."), resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarAnuncio_TamanhoMaiorQue10MB_Retorna400() {
        LivroRequest req = mock(LivroRequest.class);
        byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
        MultipartFile foto = multipartImage("foto", "image/jpeg", jpegHeader, 11 * 1024 * 1024L);

        ResponseEntity<Object> resp = controller.criarAnuncio(user, req, foto);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(
                Map.of("erro", "Arquivo '" + foto.getOriginalFilename() + "' excede o limite de 10 MB."),
                resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarAnuncio_TipoMimeInvalido_Retorna400() {
        LivroRequest req = mock(LivroRequest.class);
        byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
        MultipartFile foto = multipartImage("foto", "image/gif", jpegHeader, 1024);

        ResponseEntity<Object> resp = controller.criarAnuncio(user, req, foto);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(
                Map.of("erro",
                        "Formato inválido: '" + foto.getOriginalFilename() + "'. Apenas JPEG, PNG e WebP são aceitos."),
                resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarAnuncio_MagicBytesInvalidos_Retorna400() {
        LivroRequest req = mock(LivroRequest.class);
        byte[] invalid = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        MultipartFile foto = multipartImage("foto", "image/png", invalid, 1024);

        ResponseEntity<Object> resp = controller.criarAnuncio(user, req, foto);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(Map.of("erro", "Arquivo '" + foto.getOriginalFilename() + "' não é uma imagem válida."),
                resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void criarAnuncio_OK_QuandoValidaEServiceRetornaModelo() {
        LivroRequest dados = mock(LivroRequest.class);
        var modelo = mock(umc.exs.model.entidades.livro.Livro.class);

        byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
        MultipartFile foto = multipartImage("foto", "image/jpeg", jpegHeader, 1024);

        when(livroService.cadastrarVenda(eq(user.getUsername()), eq(dados), eq(foto))).thenReturn(modelo);

        ResponseEntity<Object> resp = controller.criarAnuncio(user, dados, foto);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        verify(livroService).cadastrarVenda(eq(user.getUsername()), eq(dados), eq(foto));
    }

    @Test
    void comprarCarrinho_ServiceThrows_RetornaBadRequest() {
        CarrinhoCompraRequest request = new CarrinhoCompraRequest();
        request.setLivroIds(List.of(1L));

        when(livroService.comprarCarrinho(eq(user.getUsername()), eq(request)))
                .thenThrow(new RuntimeException("erro interno"));

        ResponseEntity<Object> resp = controller.comprarCarrinho(user, request);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Erro ao processar o carrinho: erro interno", resp.getBody());
    }

    @Test
    void cadastrarPorIsbn_SemAuth_Retorna401() {
        ResponseEntity<Object> resp = controller.cadastrarPorIsbn(null, "978123");

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Usuário precisa estar logado.", resp.getBody());
        verifyNoInteractions(livroService);
    }

    @Test
    void cadastrarPorIsbn_QuandoServiceLançaEntityNotFound_Retorna404() {
        when(livroService.cadastrarPorIsbn("isbn")).thenThrow(new jakarta.persistence.EntityNotFoundException("nao"));

        ResponseEntity<Object> resp = controller.cadastrarPorIsbn(user, "isbn");

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("nao", resp.getBody());
    }

    @Test
    void cadastrarPorIsbn_GeneralException_Retorna404MensagemDefault() {
        when(livroService.cadastrarPorIsbn("isbn")).thenThrow(new RuntimeException("falha"));

        ResponseEntity<Object> resp = controller.cadastrarPorIsbn(user, "isbn");

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Livro não encontrado automaticamente. Preencha os dados manualmente.", resp.getBody());
    }

    @Test
    void listarVitrine_EmPromocaoTrue_ChamaMetodoPromocao() {
        var page = mock(org.springframework.data.domain.Page.class);
        when(livroService.listarPromocoesAtivasPaginado(any(), anyString()))
                .thenReturn(page);

        ResponseEntity<?> resp = controller.listarVitrine(0, 20, true, "abc");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(livroService).listarPromocoesAtivasPaginado(any(), eq("abc"));
    }

    @Test
    void listarVitrine_EmPromocaoFalse_ChamaMetodoAprovados() {
        var page = mock(org.springframework.data.domain.Page.class);
        when(livroService.listarLivrosAprovadosPaginado(any(), anyString()))
                .thenReturn(page);

        ResponseEntity<?> resp = controller.listarVitrine(0, 20, false, "abc");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(livroService).listarLivrosAprovadosPaginado(any(), eq("abc"));
    }
}
