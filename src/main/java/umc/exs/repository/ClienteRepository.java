package umc.exs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    Optional<Cliente> findByNome(String nome);
}
