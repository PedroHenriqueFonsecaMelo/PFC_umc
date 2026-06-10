package umc.exs.service.core.livros.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import umc.exs.dto.request.admin.LivroAdminRequest;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.api.ExternApi;
import umc.exs.service.core.dashboard.ListaDesejosService;
import umc.exs.service.log.LogAuditoriaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class LivroAdminServiceTest {

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private LivroAprovacaoService livroAprovacaoService;

    @Mock
    private LivroPromocaoService livroPromocaoService;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ExternApi googleBooksService;

    @Mock
    private ListaDesejosService listaDesejosService;

    @Mock
    private LogAuditoriaService logAuditoria;

    @InjectMocks
    private LivroAdminService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deveListarLivrosPendentes() {

        Livro livro = Livro.builder().titulo("Teste").build();

        when(livroRepository.findByAprovadoFalse())
                .thenReturn(List.of(livro));

        List<Livro> resultado = service.listarLivrosPendentes();

        assertEquals(1, resultado.size());
        verify(livroRepository).findByAprovadoFalse();
        verify(logAuditoria).registrarLog(anyString(), isNull(), isNull(), anyString());
    }

    @Test
    void deveListarLivrosAprovados() {

        when(livroRepository.findByAprovadoTrue())
                .thenReturn(List.of());

        List<Livro> resultado = service.listarLivrosAprovados();

        assertNotNull(resultado);
        verify(livroRepository).findByAprovadoTrue();
    }

    @Test
    void deveAprovarLivro() {

        Livro livro = Livro.builder().id(1L).build();

        when(livroAprovacaoService.aprovarLivro(eq(1L), eq(10L), any()))
                .thenReturn(livro);

        Livro resultado = service.aprovarLivro(1L, 10L, null);

        assertEquals(1L, resultado.getId());
        verify(livroAprovacaoService).aprovarLivro(eq(1L), eq(10L), any());
        verify(logAuditoria).registrarLog(anyString(), eq(10L), isNull(), anyString());
    }

    @Test
    void deveRejeitarLivro() {

        service.rejeitarLivro(1L, 10L, "RUIM", "comentario");

        verify(livroAprovacaoService)
                .rejeitarLivro(1L, 10L, "RUIM", "comentario");

        verify(logAuditoria)
                .registrarLog(anyString(), eq(10L), isNull(), contains("livroId=1"));
    }

    @Test
    void deveAdicionarLivroAdminSemPromocao() {

        LivroAdminRequest req = new LivroAdminRequest();
        req.setTitulo("Livro Teste");
        req.setAutor("Autor");
        req.setIsbn("123");
        req.setAdminId(1L);
        req.setPreco(50.0);

        Livro livroSalvo = Livro.builder().titulo("Livro Teste").build();

        when(livroRepository.save(any())).thenReturn(livroSalvo);

        Livro resultado = service.adicionarLivroAdmin(req);

        assertNotNull(resultado);
        verify(livroRepository).save(any());
        verify(logAuditoria).registrarLog(anyString(), eq(1L), isNull(), anyString());
    }

    @Test
    void deveEditarLivroAdmin() {

        Livro livro = Livro.builder()
                .id(1L)
                .titulo("Antigo")
                .emPromocao(false)
                .build();

        LivroAdminRequest req = new LivroAdminRequest();
        req.setTitulo("Novo");
        req.setAutor("Autor");
        req.setIsbn("123");
        req.setAdminId(99L);
        req.setPreco(100.0);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(livroRepository.save(any())).thenReturn(livro);

        Livro resultado = service.editarLivroAdmin(1L, req);

        assertEquals("Novo", resultado.getTitulo());
        verify(livroRepository).save(livro);
        verify(logAuditoria).registrarLog(anyString(), eq(99L), isNull(), anyString());
    }

    @Test
    void deveDeletarLivroAdmin() {

        service.deletarLivroAdmin(1L);

        verify(livroRepository).deleteById(1L);
        verify(logAuditoria).registrarLog(anyString(), isNull(), isNull(), contains("livroId=1"));
    }

    @Test
    void deveAdicionarLivroAdmin_ComDadosMinimos() {

        LivroAdminRequest req = new LivroAdminRequest();
        req.setTitulo("Livro");
        req.setAutor("Autor");
        req.setIsbn("123");
        req.setAdminId(1L);
        req.setPreco(10.0);

        when(livroRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Livro result = service.adicionarLivroAdmin(req);

        assertEquals("Livro", result.getTitulo());
        verify(livroRepository).save(any());
    }

    @Test
    void deveEditarLivroAdmin_QuandoLivroNaoExiste() {

        LivroAdminRequest req = new LivroAdminRequest();
        req.setAdminId(1L);

        when(livroRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.editarLivroAdmin(1L, req);
        });

        assertNotNull(ex);
    }

    @Test
    void deveListarLivrosPendentes_Vazio() {

        when(livroRepository.findByAprovadoFalse())
                .thenReturn(List.of());

        List<Livro> result = service.listarLivrosPendentes();

        assertTrue(result.isEmpty());
        verify(livroRepository).findByAprovadoFalse();
    }

    @Test
    void deveListarLivrosAprovados_ComDados() {

        Livro livro = Livro.builder().titulo("OK").build();

        when(livroRepository.findByAprovadoTrue())
                .thenReturn(List.of(livro));

        List<Livro> result = service.listarLivrosAprovados();

        assertEquals(1, result.size());
    }

    @Test
    void deveRejeitarLivro_VerificaParametros() {

        service.rejeitarLivro(5L, 99L, "MOTIVO", "obs");

        verify(livroAprovacaoService)
                .rejeitarLivro(5L, 99L, "MOTIVO", "obs");
    }

    @Test
    void deveListarLivrosPorLote() {

        when(livroRepository.findByLoteIdAndAprovadoFalseAndAdminAprovadorIdIsNull(1L))
                .thenReturn(List.of(Livro.builder().build()));

        List<Livro> result = service.listarLivrosPorLote(1L);

        assertEquals(1, result.size());
    }

    @Test
    void deveFalharAoAdicionarLivro_ComVendedorInvalido() {

        LivroAdminRequest req = new LivroAdminRequest();
        req.setVendedorId(99L);

        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            service.adicionarLivroAdmin(req);
        });
    }

    @Test
    void deveNotificarWishlist_QuandoEntrarEmPromocao() {

        Livro livro = Livro.builder()
                .id(1L)
                .isbn("123")
                .titulo("Livro")
                .emPromocao(false)
                .build();

        LivroAdminRequest req = new LivroAdminRequest();
        req.setIsbn("123");
        req.setTitulo("Livro");
        req.setEmPromocao(true);
        req.setPercentualDesconto(10.0);
        req.setPreco(100.0);
        req.setAdminId(1L);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(livroRepository.save(any())).thenReturn(livro);

        service.editarLivroAdmin(1L, req);

        verify(listaDesejosService).notificarClientesSeEmPromocao(
                eq("123"), anyString(), anyDouble());
    }

    @Test
    void deveAplicarInflacao() {

        Livro livro = Livro.builder()
                .id(1L)
                .precoAprovado(100.0)
                .build();

        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(livroRepository.save(any())).thenReturn(livro);

        Livro result = service.aplicarInflacaoIpcaNoPrecoAprovado(1L, 10.0);

        assertEquals(110.0, result.getPrecoAprovado());
    }

    @Test
    void deveFalharAplicarInflacao_SemPreco() {

        Livro livro = Livro.builder().id(1L).build();

        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));

        assertThrows(IllegalStateException.class, () -> {
            service.aplicarInflacaoIpcaNoPrecoAprovado(1L, 10.0);
        });
    }

    @Test
    void deveListarLivrosAprovadosPaginado_SemBusca() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Livro livro = Livro.builder().titulo("A").build();

        // Atualizado: O service agora busca a lista completa usando
        // findByAprovadoTrue() sem paginação no banco
        when(livroRepository.findByAprovadoTrue()).thenReturn(List.of(livro));

        // Act & Assert
        // Atualizado: Inclusão dos parâmetros extras de estados (null) e generos (null)
        Page<Livro> result = service.listarLivrosAprovadosPaginado(pageable, null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("A", result.getContent().get(0).getTitulo());
        verify(livroRepository).findByAprovadoTrue();
    }

    @Test
    void deveListarLivrosAprovadosPaginado_ComBusca() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Livro livro = Livro.builder()
                .titulo("Java")
                .autor("Autor")
                .isbn("123")
                .build();

        when(livroRepository.findByAprovadoTrue()).thenReturn(List.of(livro));

        // Act
        // Atualizado: Inclusão dos parâmetros extras de estados (null) e generos (null)
        Page<Livro> result = service.listarLivrosAprovadosPaginado(pageable, "java", null, null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Java", result.getContent().get(0).getTitulo());
    }

    @Test
    void deveListarPromocoesAtivas_SemBusca() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Atualizado: Mockando o novo método do repositório findPromocoesAtivas que
        // retorna List
        when(livroRepository.findPromocoesAtivas(any())).thenReturn(List.of());

        // Act
        // Atualizado: Inclusão dos parâmetros extras de estados (null) e generos (null)
        Page<Livro> result = service.listarPromocoesAtivasPaginado(pageable, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(livroRepository).findPromocoesAtivas(any());
    }

    @Test
    void deveListarPromocoesAtivas_ComBusca() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Livro livroPromo = Livro.builder()
                .titulo("Design Patterns")
                .autor("Gang of Four")
                .isbn("999")
                .build();

        // Atualizado: Configura o mock para devolver a lista com o livro em promoção
        when(livroRepository.findPromocoesAtivas(any())).thenReturn(List.of(livroPromo));

        // Act
        // Atualizado: Inclusão dos parâmetros extras de estados (null) e generos (null)
        Page<Livro> result = service.listarPromocoesAtivasPaginado(pageable, "patterns", null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Design Patterns", result.getContent().get(0).getTitulo());
    }

}