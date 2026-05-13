package umc.exs.repository.livro;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import umc.exs.model.entidades.livro.Livro;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {

    // =========================================================
    // ISBN
    // =========================================================

    Optional<Livro> findByIsbn(String isbn);

    Optional<Livro> findFirstByIsbnOrderByDataAprovacaoDesc(String isbn);

    Optional<Livro> findFirstByIsbnAndAprovadoTrueOrderByDataAprovacaoDesc(String isbn);

    // =========================================================
    // ID + APROVAÇÃO
    // =========================================================

    Optional<Livro> findByIdAndAprovadoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT l FROM Livro l WHERE l.id = :id AND l.aprovado = true")
    Optional<Livro> findByIdAndAprovadoTrueWithLock(@Param("id") Long id);

    // =========================================================
    // LOTE
    // =========================================================

    List<Livro> findByLoteId(Long loteId);

    List<Livro> findByLoteIdAndAprovadoFalse(Long loteId);

    long countByLoteIdAndAprovadoFalse(Long loteId);

    // =========================================================
    // OBRA
    // =========================================================

    List<Livro> findByObraId(Long obraId);

    // =========================================================
    // APROVAÇÃO (STATUS GERAL)
    // =========================================================

    List<Livro> findByAprovadoTrue();

    List<Livro> findByAprovadoFalse();

    long countByAprovadoTrue();

    // =========================================================
    // ANÚNCIO / DATA
    // =========================================================

    List<Livro> findByDataAnuncioAfter(LocalDateTime data);

    /**
     * Projeção usada pelo dashboard: retorna apenas as datas de anúncio,
     * sem carregar as entidades relacionadas (vendedor, lote, etc.).
     * Evita EntityNotFoundException quando o Cliente/vendedor foi deletado do banco.
     */
    @Query("SELECT l.dataAnuncio FROM Livro l WHERE l.dataAnuncio > :data AND l.dataAnuncio IS NOT NULL")
    List<LocalDateTime> findDataAnuncioAfterProjection(@Param("data") LocalDateTime data);

    // =========================================================
    // PROMOÇÕES
    // =========================================================

    List<Livro> findByAprovadoTrueAndEmPromocaoTrue();

    /** Promoções ativas (sem expiração ou ainda válidas) */
    @Query("""
                SELECT l
                FROM Livro l
                WHERE l.aprovado = true
                  AND l.emPromocao = true
                  AND (l.promocaoExpira IS NULL OR l.promocaoExpira > :agora)
            """)
    List<Livro> findPromocoesAtivas(@Param("agora") LocalDateTime agora);

    /** Promoções expiradas */
    @Query("""
                SELECT l
                FROM Livro l
                WHERE l.emPromocao = true
                  AND l.promocaoExpira IS NOT NULL
                  AND l.promocaoExpira < :agora
            """)
    List<Livro> findPromocoesExpiradas(@Param("agora") LocalDateTime agora);

    /** Total de livros aprovados enviados por um vendedor (cliente). */
    long countByVendedorIdAndAprovadoTrue(Long vendedorId);

    /** Total de livros rejeitados (não aprovados e já avaliados) de um vendedor. */
    @Query("SELECT COUNT(l) FROM Livro l WHERE l.vendedor.id = :vendedorId AND l.aprovado = false AND l.adminAprovadorId IS NOT NULL")
    long countRejeitadosByVendedorId(@Param("vendedorId") Long vendedorId);

    /**
     * Todos os livros anunciados por um cliente — vendas diretas (vendedor_id) e
     * livros enviados em lote (lote.cliente_id). Usa LEFT JOIN explícito para
     * garantir que o Hibernate não descarte registros com FK nula.
     */
    @Query("""
                SELECT l FROM Livro l
                LEFT JOIN l.vendedor v
                LEFT JOIN l.lote lo
                LEFT JOIN lo.cliente lc
                WHERE v.email = :email OR lc.email = :email
                ORDER BY l.dataAnuncio DESC
            """)
    List<Livro> findAllByVendedorEmail(@Param("email") String email);

    /**
     * Detalhe de um livro validando propriedade do cliente (segurança / LGPD).
     * Cobre vendas diretas e livros de lote via LEFT JOIN explícito.
     */
    @Query("""
                SELECT l FROM Livro l
                LEFT JOIN l.vendedor v
                LEFT JOIN l.lote lo
                LEFT JOIN lo.cliente lc
                WHERE l.id = :id
                  AND (v.email = :email OR lc.email = :email)
            """)
    Optional<Livro> findByIdAndVendedorEmail(@Param("id") Long id, @Param("email") String email);
}