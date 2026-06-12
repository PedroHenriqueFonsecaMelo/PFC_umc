package umc.exs.repository.negocios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.social.ComentarioBlog;

/** Gerencia os comentários dos posts do blog no banco de dados. */
public interface ComentarioBlogRepository extends JpaRepository<ComentarioBlog, Long> {

    /** Lista todos os comentários de um post ordenados do mais antigo ao mais recente. */
    List<ComentarioBlog> findByPostIdOrderByDataCriacaoAsc(Long postId);

    /** Conta o total de comentários de um post específico. */
    int countByPostId(Long postId);
}
