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


    Optional<Livro> findByIsbn(String isbn);

    Optional<Livro> findFirstByIsbnAndAprovadoTrueOrderByDataAprovacaoDesc(String isbn); 
    
    Optional<Livro> findFirstByIsbnOrderByDataAprovacaoDesc(String isbn);

    Optional<Livro> findByIdAndAprovadoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT l FROM Livro l WHERE l.id = :id AND l.aprovado = true")
    Optional<Livro> findByIdAndAprovadoTrueWithLock(@Param("id") Long id);

    List<Livro> findByLoteId(Long loteId);

    List<Livro> findByLoteIdAndAprovadoFalse(Long loteId);
    
    List<Livro> findByDataAnuncioAfter(LocalDateTime data);

    List<Livro> findByObraId(Long obraId);

    List<Livro> findByAprovadoTrue();

    List<Livro> findByAprovadoFalse();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Livro l WHERE l.id IN :ids AND l.aprovado = true")
    List<Livro> findAllByIdInAndAprovadoTrueWithLock (@Param("ids") List<Long> ids);

    @Query("SELECT COUNT(l) FROM Livro l WHERE l.lote.id = :loteId AND l.aprovado = false")
    long countPendingByLoteId(@Param("loteId") Long loteId);

    long countByAprovadoTrue();

}