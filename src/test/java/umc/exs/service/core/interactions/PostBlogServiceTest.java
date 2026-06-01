package umc.exs.service.core.interactions;

import static org.junit.jupiter.api.Assertions.*;
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
}
