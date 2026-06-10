package umc.exs.service.core.interactions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;
import umc.exs.repository.negocios.ComentarioBlogRepository;
import umc.exs.repository.negocios.PostBlogRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.log.AppLogger;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class PostBlogServiceTest {

    @Mock
    PostBlogRepository postBlogRepository;

    @Mock
    ComentarioBlogRepository comentarioRepository;

    @Mock
    ClienteRepository clienteRepository;

    @InjectMocks
    PostBlogService service;

    @Mock
    AppLogger appLogger;

    @Mock
    LogAuditoriaService logAuditoriaService;

    @Test
    void listarTodos_deveRetornarPostsOrdenados() {
        when(postBlogRepository.findAllByOrderByDataPublicacaoDesc()).thenReturn(List.of(new PostBlog()));
        assertEquals(1, service.listarTodos().size());
    }

    @Test
    void submeterParaRevisao_quandoRascunho_alteraStatus() {
        PostBlog post = new PostBlog();
        post.setId(1L);
        post.setStatus(StatusPost.RASCUNHO);
        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postBlogRepository.save(post)).thenReturn(post);

        PostBlog salvo = service.submeterParaRevisao(1L);
        assertEquals(StatusPost.EM_REVISAO, salvo.getStatus());
        verify(postBlogRepository).save(post);
    }

    @Test
    void toggleCurtir_quandoCurtiRetornaMapa() {
        PostBlog post = new PostBlog();
        post.setId(1L);
        post.setCurtidas(0);
        post.setStatus(StatusPost.PUBLICADO);
        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postBlogRepository.save(post)).thenReturn(post);

        var result = service.toggleCurtir(1L, false);

        assertEquals(1, result.get("curtidas"));
        assertEquals(true, result.get("curtiu"));
    }

    @Test
    void buscarPorId_quandoExiste_retornaPost() {
        PostBlog post = new PostBlog();
        post.setId(1L);

        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));

        Optional<PostBlog> resultado = service.buscarPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals(1L, resultado.get().getId());
    }

    @Test
    void listarPublicados_deveRetornarSomentePublicados() {
        when(postBlogRepository.findByStatusOrderByDataPublicacaoDesc(StatusPost.PUBLICADO))
                .thenReturn(List.of(new PostBlog(), new PostBlog()));

        List<PostBlog> lista = service.listarPublicados();

        assertEquals(2, lista.size());
    }

    @Test
    void publicar_deveAlterarStatusEData() {
        PostBlog post = new PostBlog();
        post.setId(1L);

        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postBlogRepository.save(post)).thenReturn(post);

        PostBlog resultado = service.publicar(1L);

        assertEquals(StatusPost.PUBLICADO, resultado.getStatus());
        assertNotNull(resultado.getDataPublicacao());
    }

    @Test
    void agendar_quandoDataValida_defineAgendamento() {
        PostBlog post = new PostBlog();
        post.setId(1L);

        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postBlogRepository.save(post)).thenReturn(post);

        var dataFutura = java.time.LocalDateTime.now().plusDays(1);

        PostBlog resultado = service.agendar(1L, dataFutura);

        assertEquals(StatusPost.AGENDADO, resultado.getStatus());
        assertEquals(dataFutura, resultado.getDataPublicacaoAgendada());
    }

    @Test
    void agendar_quandoDataInvalida_lancaExcecao() {
        PostBlog post = new PostBlog();
        post.setId(1L);

        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));

        var dataPassada = java.time.LocalDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> {
            service.agendar(1L, dataPassada);
        });
    }

    @Test
    void deletarPost_deveChamarRepository() {
        service.deletarPost(1L);

        verify(postBlogRepository).deleteById(1L);
    }

    @Test
    void editarPost_deveAlterarTituloEConteudo() throws Exception {
        PostBlog post = new PostBlog();
        post.setId(1L);
        post.setTitulo("Antigo");
        post.setConteudo("Antigo");

        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postBlogRepository.save(post)).thenReturn(post);

        PostBlog resultado = service.editarPost(1L, "Novo", "Novo Conteudo", null);

        assertEquals("Novo", resultado.getTitulo());
        assertEquals("Novo Conteudo", resultado.getConteudo());
    }

    @Test
    void comentar_deveCriarComentario() {
        PostBlog post = new PostBlog();
        post.setId(1L);

        when(postBlogRepository.findById(1L)).thenReturn(Optional.of(post));

        var comentario = new umc.exs.model.entidades.social.ComentarioBlog();
        when(comentarioRepository.save(any())).thenReturn(comentario);

        var resultado = service.comentar(1L, "Autor", "Texto");

        assertNotNull(resultado);
        verify(comentarioRepository).save(any());
    }

    @Test
    void listarComentarios_deveRetornarLista() {
        when(comentarioRepository.findByPostIdOrderByDataCriacaoAsc(1L))
                .thenReturn(List.of(new umc.exs.model.entidades.social.ComentarioBlog()));

        List<?> lista = service.listarComentarios(1L);

        assertEquals(1, lista.size());
    }

    @Test
    void deletarComentario_quandoAutorCorreto_deleta() {
        var comentario = new umc.exs.model.entidades.social.ComentarioBlog();
        comentario.setAutorNome("Joao");

        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));

        var cliente = new umc.exs.model.entidades.usuario.Cliente();
        cliente.setNome("Joao");

        when(clienteRepository.findByEmail("email@test.com"))
                .thenReturn(Optional.of(cliente));

        service.deletarComentario(1L, "email@test.com");

        verify(comentarioRepository).deleteById(1L);
    }

    @Test
    void deletarComentario_quandoAutorDiferente_lancaErro() {
        var comentario = new umc.exs.model.entidades.social.ComentarioBlog();
        comentario.setAutorNome("Maria");

        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));

        var cliente = new umc.exs.model.entidades.usuario.Cliente();
        cliente.setNome("Joao");

        when(clienteRepository.findByEmail("email@test.com"))
                .thenReturn(Optional.of(cliente));

        assertThrows(IllegalStateException.class, () -> {
            service.deletarComentario(1L, "email@test.com");
        });
    }
}
