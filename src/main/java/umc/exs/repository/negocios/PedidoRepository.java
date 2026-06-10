package umc.exs.repository.negocios;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.enums.StatusEnvio;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

        /** Todos os pedidos de um cliente, mais recente primeiro. */
        @EntityGraph(attributePaths = {
                        "comprador",
                        "comprador.enderecos"
        })
        List<Pedido> findByCompradorIdOrderByDataCompraDesc(Long compradorId);

        /** Pedidos de um cliente filtrando por status. */
        @EntityGraph(attributePaths = {
                        "comprador",
                        "comprador.enderecos"
        })
        List<Pedido> findByCompradorIdAndStatusEnvioOrderByDataCompraDesc(Long compradorId, StatusEnvio status);

        /** Pedidos pendentes (não entregues e não cancelados). */
        @EntityGraph(attributePaths = {
                        "comprador",
                        "comprador.enderecos"
        })
        List<Pedido> findByCompradorIdAndStatusEnvioNotInOrderByDataCompraDesc(
                        Long compradorId, List<StatusEnvio> statusExcluidos);

        /**
         * Pedidos realizados a partir de uma data (para agrupamento mensal no
         * dashboard).
         */
        List<Pedido> findByDataCompraAfter(LocalDateTime data);

        /**
         * Projeção usada pelo dashboard: retorna apenas as datas de compra,
         * sem carregar as entidades relacionadas (comprador, etc.).
         * Evita EntityNotFoundException quando o Cliente foi deletado do banco.
         */
        @Query("SELECT p.dataCompra FROM Pedido p WHERE p.dataCompra > :data")
        List<LocalDateTime> findDataCompraAfterProjection(@Param("data") LocalDateTime data);

        /**
         * Soma total de tokens gastos em pedidos (tokens utilizados).
         * Retorna null quando não há pedidos; o service trata com orElse(0.0).
         */
        @Query("SELECT SUM(p.precoLivro) FROM Pedido p")
        Double sumTokensUtilizados();

        /**
         * Estatísticas agregadas de pedidos por comprador — usado pela listagem admin
         * de clientes.
         */
        @Query("SELECT p.comprador.id, COUNT(p), COALESCE(SUM(p.precoLivro), 0.0) FROM Pedido p GROUP BY p.comprador.id")
        List<Object[]> statsGroupedByComprador();

        /** Total gasto por um cliente específico. */
        @Query("SELECT COALESCE(SUM(p.precoLivro), 0.0) FROM Pedido p WHERE p.comprador.id = :clienteId")
        Double sumGastoByClienteId(@Param("clienteId") Long clienteId);

        /** Pedidos de um cliente com filtro de data. */
        @EntityGraph(attributePaths = {
                        "comprador",
                        "comprador.enderecos"
        })
        List<Pedido> findByCompradorIdAndDataCompraAfterOrderByDataCompraDesc(
                        Long compradorId, LocalDateTime dataInicio);

        /** Verifica se um livro foi comprado (livroId é snapshot no Pedido). */
        boolean existsByLivroId(Long livroId);

        /** Verifica unicidade do código de pedido antes de persistir. */
        boolean existsByCodigoPedido(String codigoPedido);

        /** Retorna o pedido de compra de um livro, se existir. */
        java.util.Optional<Pedido> findByLivroId(Long livroId);

        @EntityGraph(attributePaths = {
                        "comprador",
                        "comprador.enderecos"
        })
        List<Pedido> findAllByOrderByDataCompraDesc();

        @EntityGraph(attributePaths = {
                        "comprador",
                        "comprador.enderecos"
        })
        @Query("select p from Pedido p where p.id = :id")
        Optional<Pedido> findComCompradorEEnderecos(@Param("id") Long id);

        @Query("""
                            SELECT p.comprador.id, COUNT(p), COALESCE(SUM(p.precoLivro), 0.0)
                            FROM Pedido p
                            WHERE p.comprador.id IN :compradorIds
                            GROUP BY p.comprador.id
                        """)
        List<Object[]> statsGroupedByCompradorIds(@Param("compradorIds") List<Long> compradorIds);
}
