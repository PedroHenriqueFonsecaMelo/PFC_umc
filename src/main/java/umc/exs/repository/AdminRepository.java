package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.exs.model.foundation.Administrador;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByEmail(String email);
}
