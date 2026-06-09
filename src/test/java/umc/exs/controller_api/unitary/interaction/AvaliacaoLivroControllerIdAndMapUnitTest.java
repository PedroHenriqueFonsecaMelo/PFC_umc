package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import umc.exs.controller.api.interaction.AvaliacaoLivroController;
import umc.exs.dto.request.cliente.ComentarioRequest;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.AvaliacaoLivroRepository;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.livros.avaliacao.LivroAvaliacaoService;

class AvaliacaoLivroControllerIdAndMapUnitTest {

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
    void salvarComentario_caminhoTriste_semAutenticacao_deveRetornar401() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        ResponseEntity<Map<String, Object>> resp = controller.salvarComentario(
                new ComentarioRequest("ISBN-1", "T", "A", "C", 5),
                auth);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(clienteRepo);
        verifyNoInteractions(livroRepo);
        verifyNoInteractions(avaliacaoRepo);
    }

    @Test
    void salvarComentario_caminhoFeliz_deveUsarIdDoSalvoNoMapResposta() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("leitor@email.com");

        Cliente leitor = mock(Cliente.class);
        when(clienteRepo.findByEmail("leitor@email.com")).thenReturn(Optional.of(leitor));

        ComentarioRequest payload = new ComentarioRequest(
                "ISBN-2",
                "Titulo2",
                "Autor2",
                "Comentario2",
                4);

        Obra obra = Obra.builder().id(55L).titulo("O").autor("A").build();
        Livro livroReferencia = Livro.builder().isbn(payload.getIsbn()).obra(obra).build();

        // força o caminho que não usa service.criarLivroReferencia
        when(livroRepo.findByIsbn(payload.getIsbn())).thenReturn(Optional.of(livroReferencia));

        AvaliacaoLivro salva = AvaliacaoLivro.builder()
                .id(123L)
                .obra(obra)
                .isbnOriginalNoAto(payload.getIsbn())
                .tituloLivro(payload.getTitulo())
                .autorLivro(payload.getAutor())
                .comentario(payload.getComentario())
                .nota(payload.getNota())
                .dataAvaliacao(LocalDateTime.now())
                .avaliador(leitor)
                .build();

        when(avaliacaoRepo.save(any(AvaliacaoLivro.class))).thenReturn(salva);

        ResponseEntity<Map<String, Object>> resp = controller.salvarComentario(payload, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("id"));

        Object idObj = resp.getBody().get("id");
        assertNotNull(idObj);
        assertEquals(123L, Long.valueOf(idObj.toString()));

        verify(avaliacaoRepo).save(any(AvaliacaoLivro.class));
        verifyNoInteractions(avaliacaoService); // porque o livroRef veio do repo
    }
}
