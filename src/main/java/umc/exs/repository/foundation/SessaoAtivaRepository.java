package umc.exs.repository.foundation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.SessaoAtiva;

@Repository
public interface SessaoAtivaRepository extends JpaRepository<SessaoAtiva, Long> {

    Optional<SessaoAtiva> findByTokenHashAndAtivaTrue(String tokenHash);

    List<SessaoAtiva> findByClienteIdAndAtivaTrue(Long clienteId);

    List<SessaoAtiva> findByClienteId(Long clienteId);
}
