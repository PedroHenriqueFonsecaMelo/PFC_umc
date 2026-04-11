package umc.exs.repository.logic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.logic.LogAuditoria;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

        List<LogAuditoria> findByIdUsuarioOrderByDataHoraDesc(Long idUsuario);

        List<LogAuditoria> findAllByOrderByDataHoraDesc();
}
