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

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM VisitaSite v")
    Long sumTotalVisitas();

    List<VisitaSite> findByDataAfter(LocalDate data);
}
