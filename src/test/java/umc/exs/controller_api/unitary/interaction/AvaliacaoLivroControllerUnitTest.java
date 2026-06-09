package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import umc.exs.controller.api.interaction.AvaliacaoLivroController;
import umc.exs.dto.request.cliente.ComentarioRequest;
import umc.exs.dto.request.livro.AvaliacaoLivroRequest;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.AvaliacaoLivroRepository;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.livros.avaliacao.LivroAvaliacaoService;

class AvaliacaoLivroControllerUnitTest {

    private LivroAvaliacaoService avaliacaoService;
    private AvaliacaoLivroRepository avaliacaoRepo;
    private LivroRepository livroRepo;
    private ClienteRepository clienteRepo;

    private AvaliacaoLivroController controller;

    @BeforeEach
    void setUp() {
        avaliacaoService = mock(LivroAvaliacaoService.class);
        avaliacaoRepo = mock(AvaliacaoLivroRepository.class);
        livroRepo = mock(LivroRepository.class);
        clienteRepo = mock(ClienteRepository.class);

        controller = new AvaliacaoLivroController(avaliacaoService, avaliacaoRepo, livroRepo, clienteRepo);
    }

    @Test
    void salvarComentario_SemAuthRetorna401() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        ResponseEntity<Map<String, Object>> resp = controller.salvarComentario(
                new ComentarioRequest("123", "T", "A", "C", 5),
                auth);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void salvarComentario_ComAuth_SemLivroNoRepo_deveChamarCriarLivroReferencia() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("leitor@email.com");

        Cliente leitor = mock(Cliente.class);
        when(clienteRepo.findByEmail("leitor@email.com")).thenReturn(Optional.of(leitor));

        String isbn = "ISBN-NEW";
        ComentarioRequest payload = new ComentarioRequest(isbn, "Titulo", "Autor", "Comentario", 4);

        Obra obraMock = mock(Obra.class);
        Livro livroReferencia = mock(Livro.class);
        when(livroReferencia.getObra()).thenReturn(obraMock);

        when(livroRepo.findByIsbn(isbn)).thenReturn(Optional.empty());
        when(avaliacaoService.criarLivroReferencia(payload)).thenReturn(livroReferencia);

        AvaliacaoLivro saved = AvaliacaoLivro.builder()
                .id(777L)
                .obra(obraMock)
                .isbnOriginalNoAto(isbn)
                .comentario(payload.getComentario())
                .nota(payload.getNota())
                .dataAvaliacao(LocalDateTime.now())
                .avaliador(leitor)
                .tituloLivro(payload.getTitulo())
                .autorLivro(payload.getAutor())
                .build();

        when(avaliacaoRepo.save(any(AvaliacaoLivro.class))).thenReturn(saved);

        ResponseEntity<Map<String, Object>> resp = controller.salvarComentario(payload, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(777L, Long.valueOf(resp.getBody().get("id").toString()));

        verify(avaliacaoService).criarLivroReferencia(payload);
    }

    @Test
    void salvarComentario_ComAuth_CriaReferenciaESalva() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("leitor@email.com");

        Cliente leitor = mock(Cliente.class);
        when(clienteRepo.findByEmail("leitor@email.com")).thenReturn(Optional.of(leitor));

        String isbn = "999";
        ComentarioRequest payload = new ComentarioRequest(isbn, "Titulo", "Autor", "Comentario", 4);

        Obra obraMock = mock(Obra.class);
        Livro livroReferencia = mock(Livro.class);
        when(livroReferencia.getObra()).thenReturn(obraMock); // Protege a linha .obra(livroReferencia.getObra()) do
                                                              // controller

        when(livroRepo.findByIsbn(isbn)).thenReturn(Optional.empty());
        when(avaliacaoService.criarLivroReferencia(payload)).thenReturn(livroReferencia);

        AvaliacaoLivro saved = AvaliacaoLivro.builder()
                .id(123L)
                .obra(obraMock)
                .isbnOriginalNoAto(isbn)
                .comentario("Comentario")
                .nota(4)
                .dataAvaliacao(LocalDateTime.now())
                .avaliador(leitor)
                .tituloLivro("Titulo")
                .autorLivro("Autor")
                .build();

        when(avaliacaoRepo.save(any(AvaliacaoLivro.class))).thenReturn(saved);

        ResponseEntity<Map<String, Object>> resp = controller.salvarComentario(payload, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(123L, Long.valueOf(resp.getBody().get("id").toString()));
    }

    @Test
    void criarAvaliacao_SemUsuarioRetorna401() {
        AvaliacaoLivroRequest dto = mock(AvaliacaoLivroRequest.class);

        ResponseEntity<Map<String, Object>> resp = controller.criarAvaliacao(null, dto);

        // CORREÇÃO: Foca na validação do Status HTTP (401)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void buscarMedia_QuandoServiceRetornaNullRetornaMedia0() {
        when(avaliacaoService.calcularMediaPorIsbn("isbnX")).thenReturn(null);

        ResponseEntity<Map<String, Object>> resp = controller.buscarMedia("isbnX");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("media"));
    }

    @Test
    void buscarDadosCentralOpiniao_QuandoLivroOficialNaoExisteMasHaHistorico() {
        String isbn = "isbn1";

        AvaliacaoLivro mockAvaliacao = avaliacaoMock(isbn);

        when(avaliacaoRepo.findByIsbnOriginalNoAtoOrderByDataAvaliacaoDesc(isbn))
                .thenReturn(List.of(mockAvaliacao));

        when(livroRepo.findFirstByIsbnOrderByDataAprovacaoDesc(isbn)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> resp = controller.buscarDadosCentralOpiniao(isbn);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("avaliacoes"));
    }

    private AvaliacaoLivro avaliacaoMock(String isbn) {
        AvaliacaoLivro a = mock(AvaliacaoLivro.class);
        when(a.getTituloLivro()).thenReturn("Titulo Hist");
        when(a.getAutorLivro()).thenReturn("Autor Hist");
        when(a.getDataAvaliacao()).thenReturn(LocalDateTime.now());
        return a;
    }
}