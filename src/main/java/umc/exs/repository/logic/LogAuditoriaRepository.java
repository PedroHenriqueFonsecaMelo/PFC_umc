package umc.exs.repository.logic;

import java.time.LocalDateTime;
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

    @Query("""
            SELECT l
            FROM LogAuditoria l
            WHERE (CAST(:emailUsuario AS string) IS NULL OR LOWER(l.emailUsuario) LIKE LOWER(CONCAT('%', CAST(:emailUsuario AS string), '%')))
              AND (CAST(:acao AS string) IS NULL OR l.acao = :acao)
              AND (CAST(:dataInicio AS localdatetime) IS NULL OR l.dataHora >= :dataInicio)
              AND (CAST(:dataFim AS localdatetime) IS NULL OR l.dataHora <= :dataFim)
            ORDER BY l.dataHora DESC
            """)
    List<LogAuditoria> buscarComFiltros(
            @Param("emailUsuario") String emailUsuario,
            @Param("acao") String acao,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("""
            SELECT DISTINCT l.acao
            FROM LogAuditoria l
            ORDER BY l.acao ASC
            """)
    List<String> buscarAcoesDistintas();

    List<LogAuditoria> findByIdUsuarioAndDataHoraAfterOrderByDataHoraDesc(
            Long clienteId,
            LocalDateTime dataCriacao);
}