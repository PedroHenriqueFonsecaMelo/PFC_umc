package umc.exs.repository.logic;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.exs.model.entidades.logic.ReporteResposta;
import java.util.List;

/** Gerencia as respostas do admin aos reportes dos usuários no banco de dados. */
public interface ReporteRespostaRepository extends JpaRepository<ReporteResposta, Long> {

    /**
     * Lista todas as respostas de um reporte ordenadas da mais antiga para a mais
     * recente, para exibição do histórico de atendimento.
     */
    List<ReporteResposta> findByReporteIdOrderByDataEnvioAsc(Long reporteId);
}
