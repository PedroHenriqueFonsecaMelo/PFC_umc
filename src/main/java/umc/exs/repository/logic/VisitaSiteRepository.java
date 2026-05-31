package umc.exs.repository.logic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.VisitaSite;

@Repository
public interface VisitaSiteRepository extends JpaRepository<VisitaSite, Long> {

    Optional<VisitaSite> findByData(LocalDate data);

    // COALESCE com literal inteiro causa ClassCastException no SQLite quando a
    // tabela está vazia.
    // Retornamos null (tabela vazia) e tratamos no service.
    @Query("SELECT SUM(v.total) FROM VisitaSite v")
    Long sumTotalVisitas();

    List<VisitaSite> findByDataAfter(LocalDate data);
}
