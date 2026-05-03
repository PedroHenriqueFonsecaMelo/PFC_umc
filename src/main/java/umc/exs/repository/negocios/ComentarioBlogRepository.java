package umc.exs.repository.negocios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.social.ComentarioBlog;

public interface ComentarioBlogRepository extends JpaRepository<ComentarioBlog, Long> {
    List<ComentarioBlog> findByPostIdOrderByDataCriacaoAsc(Long postId);
    int countByPostId(Long postId);
}
