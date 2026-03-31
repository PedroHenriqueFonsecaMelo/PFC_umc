package umc.exs.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import umc.exs.model.entidades.foundation.LivroAnuncio;

public interface LivroRepository extends JpaRepository<LivroAnuncio, Long> {

    List<LivroAnuncio> findByLoteId(Long loteId);

    List<LivroAnuncio> findByLoteIdAndAprovadoFalse(Long loteId);

    @Query("SELECT COUNT(l) FROM LivroAnuncio l WHERE l.lote.id = :loteId AND l.aprovado = false")
    long countPendingByLoteId(@Param("loteId") Long loteId);

    List<LivroAnuncio> findByAprovadoFalse();

    List<LivroAnuncio> findByAprovadoTrue();

    Optional<LivroAnuncio> findByIdAndAprovadoTrue(Long id);

    /** Livros anunciados a partir de uma data (para agrupamento mensal no dashboard). */
    List<LivroAnuncio> findByDataAnuncioAfter(LocalDateTime data);
}

