package umc.exs.repository.negocios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;

public interface PostBlogRepository extends JpaRepository<PostBlog, Long> {
    List<PostBlog> findAllByOrderByDataPublicacaoDesc();

    List<PostBlog> findByDataPublicacaoAfter(LocalDateTime data);

    List<PostBlog> findByStatusOrderByDataPublicacaoDesc(StatusPost status);

    List<PostBlog> findByStatusAndDataPublicacaoAgendadaBefore(StatusPost status, LocalDateTime dataLimite);
}
