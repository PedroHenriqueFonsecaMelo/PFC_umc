package umc.exs.repository.logic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.logic.LogAuditoria;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

        List<LogAuditoria> findByIdUsuarioOrderByDataHoraDesc(Long idUsuario);

        List<LogAuditoria> findAllByOrderByDataHoraDesc();

        @Query("SELECT l FROM LogAuditoria l WHERE " +
               "(:emailUsuario IS NULL OR LOWER(l.emailUsuario) LIKE LOWER(CONCAT('%', :emailUsuario, '%'))) AND " +
               "(:acao IS NULL OR l.acao = :acao) AND " +
               "(:dataInicio IS NULL OR l.dataHora >= :dataInicio) AND " +
               "(:dataFim IS NULL OR l.dataHora <= :dataFim) " +
               "ORDER BY l.dataHora DESC")
        List<LogAuditoria> buscarComFiltros(
            @Param("emailUsuario") String emailUsuario,
            @Param("acao") String acao,
            @Param("dataInicio") String dataInicio,
            @Param("dataFim") String dataFim
        );

        @Query("SELECT DISTINCT l.acao FROM LogAuditoria l ORDER BY l.acao ASC")
        List<String> findAcoesDistintas();
}
