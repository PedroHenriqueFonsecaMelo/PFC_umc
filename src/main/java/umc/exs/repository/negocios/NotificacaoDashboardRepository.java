package umc.exs.repository.negocios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.NotificacaoDashboard;

/** Gerencia as notificações do dashboard dos clientes no banco de dados. */
@Repository
public interface NotificacaoDashboardRepository extends JpaRepository<NotificacaoDashboard, Long> {

    /** Lista notificações não lidas de um cliente, ordenadas da mais recente. */
    List<NotificacaoDashboard> findByClienteIdAndLidaFalseOrderByDataCriacaoDesc(Long clienteId);

    /** Lista todas as notificações de um cliente ordenadas da mais recente. */
    List<NotificacaoDashboard> findByClienteIdOrderByDataCriacaoDesc(Long clienteId);

    /** Conta notificações não lidas de um cliente para o badge de alertas. */
    long countByClienteIdAndLidaFalse(Long clienteId);

    /** Marca todas as notificações de um cliente como lidas. */
    @Modifying
    @Query("UPDATE NotificacaoDashboard n SET n.lida = true WHERE n.cliente.id = :clienteId AND n.lida = false")
    int marcarTodasLidas(@Param("clienteId") Long clienteId);
}
