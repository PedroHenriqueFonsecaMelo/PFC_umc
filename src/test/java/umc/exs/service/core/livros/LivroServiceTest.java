package umc.exs.service.core.livros;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import umc.exs.dto.request.admin.AdminAprovacaoRequest;
import umc.exs.dto.request.admin.LivroAdminRequest;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.request.compra.LoteRequest;
import umc.exs.dto.request.livro.LivroRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.service.core.livros.delegado.LivroAdminService;
import umc.exs.service.core.livros.delegado.LivroAnuncioService;
import umc.exs.service.core.livros.delegado.LivroCompraService;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock
    LivroCompraService livroCompraService;

    @Mock
    LivroAnuncioService livroAnuncioService;

    @Mock
    LivroAdminService livroAdminService;

    @InjectMocks
    LivroService service;

    // ========================= COMPRA =========================

    @Test
    void realizarCompra_deveDelegar() {
        service.realizarCompra(1L, "email@test.com");
        verify(livroCompraService).realizarCompra(1L, "email@test.com");
    }

    @Test
    void comprarCarrinho_deveDelegar() {
        CarrinhoCompraRequest req = new CarrinhoCompraRequest();
        CarrinhoCompraResponse resp = mock(CarrinhoCompraResponse.class);
        when(livroCompraService.comprarCarrinho("email@test.com", req)).thenReturn(resp);

        assertSame(resp, service.comprarCarrinho("email@test.com", req));
        verify(livroCompraService).comprarCarrinho("email@test.com", req);
    }

    // ========================= ANÚNCIO =========================

    @Test
    void cadastrarVenda_deveDelegar() {
        LivroRequest dto = mock(LivroRequest.class);
        MultipartFile foto = mock(MultipartFile.class);
        Livro livro = new Livro();
        when(livroAnuncioService.cadastrarVenda("email@test.com", dto, foto)).thenReturn(livro);

        assertSame(livro, service.cadastrarVenda("email@test.com", dto, foto));
        verify(livroAnuncioService).cadastrarVenda("email@test.com", dto, foto);
    }

    @Test
    void criarLote_deveDelegar() {
        LoteRequest dto = mock(LoteRequest.class);
        List<MultipartFile> fotos = List.of(mock(MultipartFile.class));
        Lote lote = new Lote();
        when(livroAnuncioService.criarLote("email@test.com", dto, fotos)).thenReturn(lote);

        assertSame(lote, service.criarLote("email@test.com", dto, fotos));
        verify(livroAnuncioService).criarLote("email@test.com", dto, fotos);
    }

    @Test
    void listarPromocoesAtivas_deveDelegar() {
        List<Livro> livros = List.of(new Livro());
        when(livroAnuncioService.listarPromocoesAtivas()).thenReturn(livros);

        assertEquals(livros, service.listarPromocoesAtivas());
        verify(livroAnuncioService).listarPromocoesAtivas();
    }

    // ========================= ADMIN =========================

    @Test
    void listarLivrosPendentes_deveDelegar() {
        List<Livro> livros = List.of(new Livro());
        when(livroAdminService.listarLivrosPendentes()).thenReturn(livros);

        assertEquals(livros, service.listarLivrosPendentes());
        verify(livroAdminService).listarLivrosPendentes();
    }

    @Test
    void listarLivrosAprovados_deveDelegar() {
        List<Livro> livros = List.of(new Livro());
        when(livroAdminService.listarLivrosAprovados()).thenReturn(livros);

        assertEquals(livros, service.listarLivrosAprovados());
        verify(livroAdminService).listarLivrosAprovados();
    }

    /**
     * ATUALIZADO: Ajustado stubbing e verificação para contemplar os novos
     * parâmetros de filtros (List).
     */
    @Test
    void listarLivrosAprovadosPaginado_deveDelegar() {
        Pageable pageable = mock(Pageable.class);
        Page<Livro> page = mock(Page.class);
        List<String> estados = List.of("NOVO");
        List<String> generos = List.of("Ficção");

        // Configura o mock do service especialista para receber os 4 parâmetros da
        // assinatura atualizada
        when(livroAdminService.listarLivrosAprovadosPaginado(any(Pageable.class), anyString(), any(), any()))
                .thenReturn(page);

        assertSame(page, service.listarLivrosAprovadosPaginado(pageable, "teste", estados, generos));

        verify(livroAdminService).listarLivrosAprovadosPaginado(pageable, "teste", estados, generos);
    }

    /**
     * ATUALIZADO: Ajustado stubbing e verificação para contemplar os novos
     * parâmetros de filtros (List).
     */
    @Test
    void listarPromocoesAtivasPaginado_deveDelegar() {
        Pageable pageable = mock(Pageable.class);
        Page<Livro> page = mock(Page.class);
        List<String> estados = List.of("BOM");
        List<String> generos = List.of("Terror");

        when(livroAdminService.listarPromocoesAtivasPaginado(any(Pageable.class), anyString(), any(), any()))
                .thenReturn(page);

        assertSame(page, service.listarPromocoesAtivasPaginado(pageable, "teste", estados, generos));
        verify(livroAdminService).listarPromocoesAtivasPaginado(pageable, "teste", estados, generos);
    }

    /**
     * NOVO TESTE: Valida o comportamento de delegação da busca de gêneros únicos da
     * vitrine.
     */
    @Test
    void listarGenerosUnicosCadastrados_deveDelegar() {
        List<String> generosEsperados = List.of("Drama", "Suspense");
        when(livroAdminService.listarGenerosUnicosCadastrados()).thenReturn(generosEsperados);

        List<String> resultado = service.listarGenerosUnicosCadastrados();

        assertEquals(generosEsperados, resultado);
        verify(livroAdminService).listarGenerosUnicosCadastrados();
    }

    @Test
    void listarLivrosPorLote_deveDelegar() {
        when(livroAdminService.listarLivrosPorLote(1L)).thenReturn(List.of(new Livro()));
        assertEquals(1, service.listarLivrosPorLote(1L).size());
        verify(livroAdminService).listarLivrosPorLote(1L);
    }

    @Test
    void aprovarLivro_deveDelegar() {
        AdminAprovacaoRequest dto = new AdminAprovacaoRequest();
        Livro livro = new Livro();
        when(livroAdminService.aprovarLivro(1L, 2L, dto)).thenReturn(livro);

        assertSame(livro, service.aprovarLivro(1L, 2L, dto));
        verify(livroAdminService).aprovarLivro(1L, 2L, dto);
    }

    @Test
    void rejeitarLivro_deveDelegar() {
        doNothing().when(livroAdminService).rejeitarLivro(anyLong(), anyLong(), anyString(), anyString());
        service.rejeitarLivro(1L, 2L, "REJEITADO", "coment");
        verify(livroAdminService).rejeitarLivro(1L, 2L, "REJEITADO", "coment");
    }

    @Test
    void deletarLivroAdmin_deveDelegar() {
        doNothing().when(livroAdminService).deletarLivroAdmin(1L);
        service.deletarLivroAdmin(1L);
        verify(livroAdminService).deletarLivroAdmin(1L);
    }

    @Test
    void adicionarLivroAdmin_deveDelegar() {
        LivroAdminRequest req = mock(LivroAdminRequest.class);
        Livro livro = new Livro();
        when(livroAdminService.adicionarLivroAdmin(req)).thenReturn(livro);

        assertSame(livro, service.adicionarLivroAdmin(req));
        verify(livroAdminService).adicionarLivroAdmin(req);
    }

    @Test
    void editarLivroAdmin_deveDelegar() {
        LivroAdminRequest req = mock(LivroAdminRequest.class);
        Livro livro = new Livro();
        when(livroAdminService.editarLivroAdmin(1L, req)).thenReturn(livro);

        assertSame(livro, service.editarLivroAdmin(1L, req));
        verify(livroAdminService).editarLivroAdmin(1L, req);
    }

    @Test
    void buscarPorIdAtivo_deveDelegar() {
        when(livroAnuncioService.buscarPorIdAtivo(1L)).thenReturn(new Livro());
        assertNotNull(service.buscarPorIdAtivo(1L));
        verify(livroAnuncioService).buscarPorIdAtivo(1L);
    }

    @Test
    void cadastrarPorIsbn_deveDelegar() {
        when(livroAnuncioService.cadastrarPorIsbn("isbn")).thenReturn(new Livro());
        assertNotNull(service.cadastrarPorIsbn("isbn"));
        verify(livroAnuncioService).cadastrarPorIsbn("isbn");
    }
}