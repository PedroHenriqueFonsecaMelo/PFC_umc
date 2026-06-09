package umc.exs.service.core.livros.avaliacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.request.cliente.ComentarioRequest;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.AvaliacaoLivroRepository;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.livro.ObraRpository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LivroAvaliacaoServiceTest {

    @Mock
    private AvaliacaoLivroRepository avaliacaoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private ObraRpository obraRepo;

    @Mock
    private LogAuditoriaService logAuditoria;

    @Mock
    private GamificacaoService gamificacaoService;

    @InjectMocks
    private LivroAvaliacaoService service;

    @Test
    void calcularMediaPorIsbn_quandoExisteLivro_eObraNaoNula_deveRetornarMedia() {
        String isbn = "ISBN-1";

        Obra obra = Obra.builder().id(10L).titulo("O1").autor("A1").build();
        Livro livro = Livro.builder().isbn(isbn).obra(obra).build();

        when(livroRepository.findByIsbn(isbn)).thenReturn(Optional.of(livro));
        when(avaliacaoRepository.getAverageRatingByObraId(10L)).thenReturn(4.5d);

        Double media = service.calcularMediaPorIsbn(isbn);

        assertNotNull(media);
        assertEquals(4.5d, media);

        verify(avaliacaoRepository).getAverageRatingByObraId(10L);
        verify(logAuditoria).registrarLog(eq("LIVRO_AVALIACAO_MEDIA_CALCULADA"), isNull(), isNull(), contains("ISBN="));
    }

    @Test
    void calcularMediaPorIsbn_quandoExisteLivro_eObraNula_deveRetornarNull() {
        String isbn = "ISBN-OBRA-NULA";

        Livro livro = Livro.builder().isbn(isbn).obra(null).build();
        when(livroRepository.findByIsbn(isbn)).thenReturn(Optional.of(livro));

        Double media = service.calcularMediaPorIsbn(isbn);

        assertNull(media);
        verifyNoInteractions(avaliacaoRepository);
    }

    @Test
    void calcularMediaPorIsbn_quandoNaoExisteLivro_deveRetornarNull() {
        String isbn = "ISBN-NAOEXISTE";

        when(livroRepository.findByIsbn(isbn)).thenReturn(Optional.empty());

        Double media = service.calcularMediaPorIsbn(isbn);

        assertNull(media);
        verifyNoInteractions(avaliacaoRepository);
    }

    @Test
    void criarLivroReferencia_quandoLivroExiste_deveRetornarLivroSemCriarObra() {
        ComentarioRequest payload = new ComentarioRequest("ISBN-2", "Titulo2", "Autor2", "Comentario", 4);

        Obra obraExistente = Obra.builder().id(20L).titulo("T").autor("A").build();
        Livro livroExistente = Livro.builder().isbn(payload.getIsbn()).titulo(payload.getTitulo()).obra(obraExistente)
                .build();

        when(livroRepository.findByIsbn(payload.getIsbn())).thenReturn(Optional.of(livroExistente));

        Livro result = service.criarLivroReferencia(payload);

        assertSame(livroExistente, result);
        verify(livroRepository, never()).save(any(Livro.class));
        verify(obraRepo, never()).save(any(Obra.class));

        verify(logAuditoria).registrarLog(eq("LIVRO_REFERENCIA_CRIADO"), isNull(), isNull(), contains("ISBN="));
    }

    @Test
    void criarLivroReferencia_quandoLivroNaoExiste_deveCriarObraELivro() {
        ComentarioRequest payload = new ComentarioRequest("ISBN-3", "Titulo3", "Autor3", "Comentario", 4);

        when(livroRepository.findByIsbn(payload.getIsbn())).thenReturn(Optional.empty());

        Obra obraSalva = Obra.builder().id(30L).titulo(payload.getTitulo()).autor(payload.getAutor()).build();
        when(obraRepo.save(any(Obra.class))).thenReturn(obraSalva);

        Livro livroSalvo = Livro.builder()
                .isbn(payload.getIsbn())
                .titulo(payload.getTitulo())
                .obra(obraSalva)
                .build();
        when(livroRepository.save(any(Livro.class))).thenReturn(livroSalvo);

        Livro result = service.criarLivroReferencia(payload);

        assertNotNull(result);
        assertEquals(payload.getIsbn(), result.getIsbn());
        assertEquals(payload.getTitulo(), result.getTitulo());
        assertNotNull(result.getObra());
        assertSame(obraSalva, result.getObra());

        ArgumentCaptor<Obra> obraCaptor = ArgumentCaptor.forClass(Obra.class);
        verify(obraRepo).save(obraCaptor.capture());
        assertEquals(payload.getTitulo(), obraCaptor.getValue().getTitulo());
        assertEquals(payload.getAutor(), obraCaptor.getValue().getAutor());

        ArgumentCaptor<Livro> livroCaptor = ArgumentCaptor.forClass(Livro.class);
        verify(livroRepository).save(livroCaptor.capture());
        assertEquals(payload.getIsbn(), livroCaptor.getValue().getIsbn());
        assertEquals(payload.getTitulo(), livroCaptor.getValue().getTitulo());
        assertNotNull(livroCaptor.getValue().getObra());
        assertSame(obraSalva, livroCaptor.getValue().getObra());

        verify(logAuditoria).registrarLog(eq("LIVRO_REFERENCIA_CRIADO"), isNull(), isNull(), contains("ISBN="));
    }

}
