package umc.exs.repository.livro;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import umc.exs.model.entidades.livro.Livro;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {


    Optional<Livro> findByIsbn(String isbn);

    Optional<Livro> findFirstByIsbnAndAprovadoTrueOrderByDataAprovacaoDesc(String isbn);

    List<Livro> findByLoteId(Long loteId);

    List<Livro> findByLoteIdAndAprovadoFalse(Long loteId);

    @Query("SELECT COUNT(l) FROM Livro l WHERE l.lote.id = :loteId AND l.aprovado = false")
    long countPendingByLoteId(@Param("loteId") Long loteId);

    List<Livro> findByAprovadoFalse();

    List<Livro> findByAprovadoTrue();

    long countByAprovadoTrue();

    Optional<Livro> findByIdAndAprovadoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Livro l WHERE l.id = :id AND l.aprovado = true")
    Optional<Livro> findByIdAndAprovadoTrueWithLock(@Param("id") Long id);

    List<Livro> findByDataAnuncioAfter(LocalDateTime data);

    List<Livro> findByObraId(Long obraId);

    Optional<Livro> findFirstByIsbnOrderByDataAprovacaoDesc(String isbn);

    List<Livro> findByAprovadoTrueAndEmPromocaoTrue();

    /** Retorna apenas promoções ainda válidas (sem expiração ou expiração futura). */
    @Query("SELECT l FROM Livro l WHERE l.aprovado = true AND l.emPromocao = true AND (l.promocaoExpira IS NULL OR l.promocaoExpira > :agora)")
    List<Livro> findPromocoesAtivas(@Param("agora") LocalDateTime agora);

    @Query("SELECT l FROM Livro l WHERE l.emPromocao = true AND l.promocaoExpira IS NOT NULL AND l.promocaoExpira < :agora")
    List<Livro> findPromocoesExpiradas(@Param("agora") LocalDateTime agora);
}