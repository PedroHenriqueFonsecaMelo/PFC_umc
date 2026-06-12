package umc.exs.repository.logic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.VisitaSite;

/**
 * Gerencia o registro de visitas diárias ao site no banco, usado pelo
 * dashboard para exibir o total de acessos à plataforma.
 */
@Repository
public interface VisitaSiteRepository extends JpaRepository<VisitaSite, Long> {

    /** Busca o registro de visitas de um dia específico para incrementar o contador. */
    Optional<VisitaSite> findByData(LocalDate data);

    /** Soma o total de visitas de todos os dias; retorna null se a tabela estiver vazia. */
    // COALESCE com literal inteiro causa ClassCastException no SQLite quando a
    // tabela está vazia.
    // Retornamos null (tabela vazia) e tratamos no service.
    @Query("SELECT SUM(v.total) FROM VisitaSite v")
    Long sumTotalVisitas();

    /** Lista registros de visitas a partir de uma data para geração de gráficos históricos. */
    List<VisitaSite> findByDataAfter(LocalDate data);
}
