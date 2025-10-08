package umc.exs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNome(String nome);
}
