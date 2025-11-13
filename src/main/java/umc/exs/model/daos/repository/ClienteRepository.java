package umc.exs.model.daos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.usuario.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @EntityGraph(attributePaths = { "enderecos", "cartoes" })
    Optional<Cliente> findByEmail(String email);

    // Permitir múltiplos registros com mesmo email:
    List<Cliente> findAllByEmail(String email);

    Optional<Cliente> findByCpf(String cpf);

    // Query personalizada para buscar por email + id
    @Query("SELECT c FROM Cliente c WHERE c.email = :email AND c.id = :id")
    Optional<Cliente> findByEmailAndId(@Param("email") String email, @Param("id") Long id);

    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.cartoes WHERE c.id = :id")
    Optional<Cliente> findByIdWithCartoes(@Param("id") Long id);

    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.enderecos WHERE c.id = :id")
    Optional<Cliente> findByIdWithEnderecos(@Param("id") Long id);
}
