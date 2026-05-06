package umc.exs.service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;
import umc.exs.repository.negocios.PostBlogRepository;

/**
 * Publica automaticamente posts agendados quando a data/hora chegou.
 * Roda a cada 60 segundos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogSchedulerService {

    private final PostBlogRepository postBlogRepository;

    @Transactional
    @Scheduled(fixedDelay = 60_000)
    public void publicarPostsAgendados() {
        List<PostBlog> agendados = postBlogRepository
                .findByStatusAndDataPublicacaoAgendadaBefore(StatusPost.AGENDADO, LocalDateTime.now());

        if (agendados.isEmpty()) {
            return;
        }

        log.info("BlogScheduler: publicando {} post(s) agendado(s).", agendados.size());
        for (PostBlog post : agendados) {
            post.setStatus(StatusPost.PUBLICADO);
            post.setDataPublicacao(post.getDataPublicacaoAgendada());
            postBlogRepository.save(post);
            log.info("Post ID {} '{}' publicado automaticamente.", post.getId(), post.getTitulo());
        }
    }
}
