package umc.exs.repository.negocios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;

/**
 * Gerencia os posts do blog no banco de dados, com suporte a filtros por
 * status e data.
 */
public interface PostBlogRepository extends JpaRepository<PostBlog, Long> {

    /** Lista todos os posts do mais recente ao mais antigo. */
    List<PostBlog> findAllByOrderByDataPublicacaoDesc();

    /** Lista posts publicados após uma data, usado para estatísticas do dashboard. */
    List<PostBlog> findByDataPublicacaoAfter(LocalDateTime data);

    /** Lista posts filtrados por status (PUBLICADO, RASCUNHO, AGENDADO). */
    List<PostBlog> findByStatusOrderByDataPublicacaoDesc(StatusPost status);

    /**
     * Busca posts agendados cuja data de publicação já passou, usado pelo
     * scheduler de publicação automática.
     */
    List<PostBlog> findByStatusAndDataPublicacaoAgendadaBefore(StatusPost status, LocalDateTime dataLimite);
}
