package umc.exs.repository.negocios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.social.PostBlog;

public interface PostBlogRepository extends JpaRepository<PostBlog, Long> {
    List<PostBlog> findAllByOrderByDataPublicacaoDesc();

    List<PostBlog> findByDataPublicacaoAfter(LocalDateTime data);
}
