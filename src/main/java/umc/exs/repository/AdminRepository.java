package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.foundation.Administrador;

public interface AdminRepository extends JpaRepository<Administrador, Long> {
    
}
