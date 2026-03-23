package umc.exs.repository;

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

    // Carrega o cliente com todas as associações em uma única consulta (Eager Loading planejado)
    @EntityGraph(attributePaths = { "enderecos", "cartoes" })
    Optional<Cliente> findByEmail(String email);

    // Se o email não for único, este método é o correto para listagens
    List<Cliente> findAllByEmail(String email);

    Optional<Cliente> findByCpf(String cpf);

    // Query para verificar se o ID pertence àquele e-mail (Segurança extra em filtros)
    @Query("SELECT c FROM Cliente c WHERE c.email = :email AND c.id = :id")
    Optional<Cliente> findByEmailAndId(@Param("email") String email, @Param("id") Long id);

    // Busca otimizada: Traz o cliente e já "puxa" os cartões da memória
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.cartoes WHERE c.id = :id")
    Optional<Cliente> findByIdWithCartoes(@Param("id") Long id);

    // Busca otimizada: Traz o cliente e já "puxa" os endereços da memória
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.enderecos WHERE c.id = :id")
    Optional<Cliente> findByIdWithEnderecos(@Param("id") Long id);

    boolean existsByEmail(String email);
    
    boolean existsByCpf(String cpf);
}