package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.foundation.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    
}
