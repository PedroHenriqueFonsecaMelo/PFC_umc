package umc.exs.repository.negocios;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.enums.CategoriaForum;

/**
 * Gerencia os tópicos do fórum no banco, com paginação, filtros e carregamento
 * eager do autor via EntityGraph para evitar LazyInitializationException.
 */
@Repository
public interface TopicoForumRepository extends JpaRepository<TopicoForum, Long> {

        // Lista paginada — @EntityGraph carrega o autor em JOIN, evitando
        // LazyInitializationException
        // com open-in-view=false

        /** Lista todos os tópicos paginados com autor carregado. */
        @Override
        @EntityGraph(attributePaths = { "autor" })
        Page<TopicoForum> findAll(Pageable pageable);

        /** Lista tópicos de uma categoria paginados. */
        @EntityGraph(attributePaths = { "autor" })
        Page<TopicoForum> findByCategoria(CategoriaForum categoria, Pageable pageable);

        /** Busca tópicos por título ignorando maiúsculas/minúsculas. */
        @EntityGraph(attributePaths = { "autor" })
        Page<TopicoForum> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

        /** Busca por título e categoria simultaneamente. */
        @EntityGraph(attributePaths = { "autor" })
        Page<TopicoForum> findByTituloContainingIgnoreCaseAndCategoria(
                        String titulo, CategoriaForum categoria, Pageable pageable);

        // Detalhe do tópico — carrega autor + respostas + autor de cada resposta em uma
        // query
        /** Busca tópico com autor, respostas e autores das respostas em uma única query para evitar N+1. */
        @Query("""
                        SELECT DISTINCT t FROM TopicoForum t
                        JOIN FETCH t.autor
                        LEFT JOIN FETCH t.respostas r
                        LEFT JOIN FETCH r.autor
                        WHERE t.id = :id
                        """)
        Optional<TopicoForum> findByIdWithRespostas(@Param("id") Long id);

        /** Incrementa o contador de visualizações diretamente no banco sem carregar a entidade. */
        @Modifying
        @Query("UPDATE TopicoForum t SET t.visualizacoes = t.visualizacoes + 1 WHERE t.id = :id")
        void incrementarVisualizacoes(@Param("id") Long id);

        /** Conta tópicos criados por um autor (cliente). */
        long countByAutorId(Long autorId);
}
