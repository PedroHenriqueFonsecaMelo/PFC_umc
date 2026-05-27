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

public interface ReservaCheckoutRepository extends JpaRepository<ReservaCheckout, Long> {

    // Busca reserva ativa de outro usuário para este livro
    @Query("SELECT r FROM ReservaCheckout r WHERE r.livroId = :livroId AND r.clienteId != :clienteId AND r.expiraEm > :agora")
    Optional<ReservaCheckout> findReservaAtivaDeOutro(
        @Param("livroId") Long livroId,
        @Param("clienteId") Long clienteId,
        @Param("agora") LocalDateTime agora);

    // Busca reserva do próprio usuário para este livro
    Optional<ReservaCheckout> findByLivroIdAndClienteId(Long livroId, Long clienteId);

    // Remove reservas expiradas (cron de limpeza)
    @Modifying
    @Transactional
    @Query("DELETE FROM ReservaCheckout r WHERE r.expiraEm < :agora")
    void deleteExpiradas(@Param("agora") LocalDateTime agora);

    // Todas as reservas ativas de um cliente
    List<ReservaCheckout> findByClienteId(Long clienteId);
}
