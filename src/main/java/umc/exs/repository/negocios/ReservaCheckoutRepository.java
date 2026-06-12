package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import umc.exs.model.entidades.foundation.ReservaCheckout;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gerencia as reservas temporárias de livros no checkout, garantindo que o
 * mesmo exemplar não seja comprado por dois clientes simultaneamente.
 */
public interface ReservaCheckoutRepository extends JpaRepository<ReservaCheckout, Long> {

    /** Verifica se outro cliente já reservou este livro, usado no lock de checkout. */
    // Busca reserva ativa de outro usuário para este livro
    @Query("SELECT r FROM ReservaCheckout r WHERE r.livroId = :livroId AND r.clienteId != :clienteId AND r.expiraEm > :agora")
    Optional<ReservaCheckout> findReservaAtivaDeOutro(
            @Param("livroId") Long livroId,
            @Param("clienteId") Long clienteId,
            @Param("agora") LocalDateTime agora);

    /** Busca a reserva ativa do próprio cliente para um livro específico. */
    // Busca reserva do próprio usuário para este livro
    Optional<ReservaCheckout> findByLivroIdAndClienteId(Long livroId, Long clienteId);

    /** Remove reservas vencidas, executado pelo scheduler a cada 60 segundos. */
    // Remove reservas expiradas (cron de limpeza)
    @Modifying
    @Transactional
    @Query("DELETE FROM ReservaCheckout r WHERE r.expiraEm < :agora")
    void deleteExpiradas(@Param("agora") LocalDateTime agora);

    /** Lista todas as reservas ativas de um cliente no momento do checkout. */
    // Todas as reservas ativas de um cliente
    List<ReservaCheckout> findByClienteId(Long clienteId);
}
