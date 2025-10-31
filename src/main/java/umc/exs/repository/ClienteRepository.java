package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.exs.model.entidades.Cliente;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByEmailAndSenha(String email, String senha);
}
