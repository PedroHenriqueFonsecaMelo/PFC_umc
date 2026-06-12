package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.usuario.Cliente;

import java.util.List;
import java.util.Optional;

/**
 * Gerencia os lotes de livros submetidos pelos vendedores no banco de dados.
 */
public interface LoteRepository extends JpaRepository<Lote, Long> {

    /** Lista lotes por status (PENDENTE, APROVADO, etc.). */
    List<Lote> findByStatus(Lote.LoteStatus status);

    /** Lista lotes por status com cliente carregado (evita N+1 queries). */
    @Query("SELECT l FROM Lote l JOIN FETCH l.cliente WHERE l.status = :status ORDER BY l.dataCriacao ASC")
    List<Lote> findByStatusWithCliente(@Param("status") Lote.LoteStatus status);

    /** Busca lote por ID com cliente carregado. */
    @Query("SELECT l FROM Lote l JOIN FETCH l.cliente WHERE l.id = :id")
    Optional<Lote> findByIdWithCliente(@Param("id") Long id);

    /** Conta o total de lotes de um cliente. */
    long countByClienteId(Long clienteId);

    /** Conta lotes de um cliente por status. */
    @Query("SELECT COUNT(l) FROM Lote l WHERE l.cliente.id = :clienteId AND l.status = :status")
    long countByClienteIdAndStatus(@Param("clienteId") Long clienteId, @Param("status") Lote.LoteStatus status);

    /** Lista lotes de um cliente por status. */
    List<Lote> findByClienteAndStatus(Cliente cliente, Lote.LoteStatus status);

    /** Conta lotes de um cliente com determinados status. */
    @Query("SELECT COUNT(l) FROM Lote l WHERE l.cliente.id = :clienteId AND l.status IN :statuses")
    long countByClienteIdAndStatusIn(@Param("clienteId") Long clienteId,
            @Param("statuses") java.util.List<Lote.LoteStatus> statuses);
}
