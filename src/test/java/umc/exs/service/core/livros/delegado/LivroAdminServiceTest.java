package umc.exs.service.core.livros.delegado;

import static org.junit.jupiter.api.Assertions.*;
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

}