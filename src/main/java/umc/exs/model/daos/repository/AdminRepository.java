package umc.exs.model.daos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.Administrador;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByEmail(String email);

    public Optional<Administrador> findByEmailAndId(String email, Long id);
}
