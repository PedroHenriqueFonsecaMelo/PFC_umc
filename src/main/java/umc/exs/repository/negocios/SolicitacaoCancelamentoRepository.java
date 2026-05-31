package umc.exs.repository.negocios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.model.enums.StatusSolicitacao;

@Repository
public interface SolicitacaoCancelamentoRepository extends JpaRepository<SolicitacaoCancelamento, Long> {

    List<SolicitacaoCancelamento> findAllByOrderByDataSolicitacaoDesc();

    List<SolicitacaoCancelamento> findByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao status);

    List<SolicitacaoCancelamento> findByClienteIdOrderByDataSolicitacaoDesc(Long clienteId);

    Optional<SolicitacaoCancelamento> findByPedidoIdAndStatus(Long pedidoId, StatusSolicitacao status);

    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusSolicitacao status);

    long countByStatus(StatusSolicitacao status);

    long countByClienteId(Long clienteId);
}
