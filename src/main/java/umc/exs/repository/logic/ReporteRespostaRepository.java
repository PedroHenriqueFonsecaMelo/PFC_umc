package umc.exs.repository.logic;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.exs.model.entidades.logic.ReporteResposta;
import java.util.List;

public interface ReporteRespostaRepository extends JpaRepository<ReporteResposta, Long> {
    List<ReporteResposta> findByReporteIdOrderByDataEnvioAsc(Long reporteId);
}
