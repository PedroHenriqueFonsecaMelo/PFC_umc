package umc.exs.repository.logic;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.logic.LogAuditoria;

/**
 * Gerencia os logs de auditoria no banco, com suporte a filtros por usuário,
 * ação e período para o painel administrativo.
 */
@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    /** Lista logs de um usuário específico ordenados do mais recente ao mais antigo. */
    List<LogAuditoria> findByIdUsuarioOrderByDataHoraDesc(Long idUsuario);

    /** Lista todos os logs ordenados do mais recente ao mais antigo. */
    List<LogAuditoria> findAllByOrderByDataHoraDesc();

    /** Busca logs com filtros opcionais de e-mail, ação e intervalo de datas; parâmetros nulos são ignorados. */
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

    /** Retorna lista de tipos de ação únicos para popular o filtro de ações no painel. */
    @Query("""
            SELECT DISTINCT l.acao
            FROM LogAuditoria l
            ORDER BY l.acao ASC
            """)
    List<String> buscarAcoesDistintas();

    /** Lista logs de um usuário a partir de uma data específica. */
    List<LogAuditoria> findByIdUsuarioAndDataHoraAfterOrderByDataHoraDesc(
            Long clienteId,
            LocalDateTime dataCriacao);
}