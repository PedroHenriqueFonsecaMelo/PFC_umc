package umc.exs.repository.negocios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.enums.StatusEnvio;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Todos os pedidos de um cliente, mais recente primeiro. */
    List<Pedido> findByCompradorIdOrderByDataCompraDesc(Long compradorId);

    /** Pedidos de um cliente filtrando por status. */
    List<Pedido> findByCompradorIdAndStatusEnvioOrderByDataCompraDesc(Long compradorId, StatusEnvio status);

    /** Pedidos pendentes (não entregues e não cancelados). */
    List<Pedido> findByCompradorIdAndStatusEnvioNotInOrderByDataCompraDesc(
            Long compradorId, List<StatusEnvio> statusExcluidos);

    /**
     * Pedidos realizados a partir de uma data (para agrupamento mensal no
     * dashboard).
     */
    List<Pedido> findByDataCompraAfter(LocalDateTime data);

    /** Soma total de tokens gastos em pedidos (tokens utilizados). */
    @Query("SELECT COALESCE(SUM(p.precoLivro), 0) FROM Pedido p")
    Double sumTokensUtilizados();
}
