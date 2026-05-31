package umc.exs.service.scheduler;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;
import umc.exs.repository.negocios.PostBlogRepository;

@ExtendWith(MockitoExtension.class)
class BlogSchedulerServiceTest {

    @Mock
    PostBlogRepository postBlogRepository;

    @InjectMocks
    BlogSchedulerService service;

    @Test
    void publicarPostsAgendados_devePublicarPosts() {
        PostBlog post = new PostBlog();
        post.setId(1L);
        post.setTitulo("Título");
        post.setStatus(StatusPost.AGENDADO);
        post.setDataPublicacaoAgendada(LocalDateTime.now().minusMinutes(1));

        when(postBlogRepository.findByStatusAndDataPublicacaoAgendadaBefore(
                eq(StatusPost.AGENDADO),
                any(LocalDateTime.class))).thenReturn(List.of(post));

        when(postBlogRepository.save(post)).thenReturn(post);

        service.publicarPostsAgendados();

        assertEquals(StatusPost.PUBLICADO, post.getStatus());
        verify(postBlogRepository).save(post);
    }
}
