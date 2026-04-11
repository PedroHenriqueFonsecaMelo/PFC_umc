package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.usuario.Cliente;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findByStatus(Lote.LoteStatus status);

    @Query("SELECT COUNT(l) FROM Lote l WHERE l.cliente.id = :clienteId AND l.status = :status")
    long countByClienteIdAndStatus(@Param("clienteId") Long clienteId, @Param("status") Lote.LoteStatus status);

    List<Lote> findByClienteAndStatus(Cliente cliente, Lote.LoteStatus status);
}
