package umc.exs.repository.negocios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.NotificacaoDashboard;

@Repository
public interface NotificacaoDashboardRepository extends JpaRepository<NotificacaoDashboard, Long> {

    List<NotificacaoDashboard> findByClienteIdAndLidaFalseOrderByDataCriacaoDesc(Long clienteId);

    List<NotificacaoDashboard> findByClienteIdOrderByDataCriacaoDesc(Long clienteId);

    long countByClienteIdAndLidaFalse(Long clienteId);
}
