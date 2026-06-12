package umc.exs.repository.negocios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.model.enums.StatusSolicitacao;

/** Gerencia as solicitações de cancelamento de pedidos no banco de dados. */
@Repository
public interface SolicitacaoCancelamentoRepository extends JpaRepository<SolicitacaoCancelamento, Long> {

    /** Lista todas as solicitações da mais recente à mais antiga para o painel admin. */
    List<SolicitacaoCancelamento> findAllByOrderByDataSolicitacaoDesc();

    /** Lista solicitações filtradas por status (PENDENTE, APROVADO, RECUSADO). */
    List<SolicitacaoCancelamento> findByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao status);

    /** Lista solicitações de um cliente específico. */
    List<SolicitacaoCancelamento> findByClienteIdOrderByDataSolicitacaoDesc(Long clienteId);

    /** Busca solicitação de um pedido com status específico. */
    Optional<SolicitacaoCancelamento> findByPedidoIdAndStatus(Long pedidoId, StatusSolicitacao status);

    /** Verifica se existe solicitação pendente para um pedido. */
    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusSolicitacao status);

    /** Conta solicitações por status, usado no badge de pendências do painel admin. */
    long countByStatus(StatusSolicitacao status);

    /** Conta o total de cancelamentos solicitados por um cliente. */
    long countByClienteId(Long clienteId);
}
