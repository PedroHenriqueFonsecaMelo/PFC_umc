package umc.exs.repository.usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.usuario.Cliente;

/**
 * Gerencia os clientes no banco, com queries otimizadas via EntityGraph para
 * evitar N+1 e suporte a soft delete via campo ativo.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Carrega o cliente com todas as associações em uma única consulta (Eager
    // Loading planejado)
    @EntityGraph(attributePaths = { "enderecos", "cartoes" })
    Optional<Cliente> findByEmail(String email);

    // Busca apenas clientes ativos (soft delete)
    @EntityGraph(attributePaths = { "enderecos", "cartoes" })
    Optional<Cliente> findByEmailAndAtivoTrue(String email);

    // Se o email não for único, este método é o correto para listagens
    List<Cliente> findAllByEmail(String email);

    /** Busca cliente pelo CPF. */
    Optional<Cliente> findByCpf(String cpf);

    // Verifica unicidade apenas entre usuários ativos (soft delete)
    /** Verifica unicidade de e-mail entre clientes ativos para evitar cadastro duplicado. */
    boolean existsByEmailAndAtivoTrue(String email);

    /** Verifica unicidade de CPF entre ativos. */
    boolean existsByCpfAndAtivoTrue(String cpf);

    // Query para verificar se o ID pertence àquele e-mail (Segurança extra em
    // filtros)
    @Query("SELECT c FROM Cliente c WHERE c.email = :email AND c.id = :id")
    Optional<Cliente> findByEmailAndId(@Param("email") String email, @Param("id") Long id);

    // Busca otimizada: Traz o cliente e já "puxa" os cartões da memória
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.cartoes WHERE c.id = :id")
    Optional<Cliente> findByIdWithCartoes(@Param("id") Long id);

    // Busca otimizada: Traz o cliente e já "puxa" os endereços da memória
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.enderecos WHERE c.id = :id")
    Optional<Cliente> findByIdWithEnderecos(@Param("id") Long id);

    /** Verifica se e-mail já existe no banco. */
    boolean existsByEmail(String email);

    /** Verifica se CPF já existe no banco. */
    boolean existsByCpf(String cpf);

    /**
     * Clientes cadastrados a partir de uma data (para agrupamento mensal no
     * dashboard).
     */
    List<Cliente> findByDataCriacaoAfter(LocalDateTime data);

    /** Soma de todos os saldos ativos na plataforma (tokens em circulação). */
    @Query("SELECT COALESCE(SUM(c.saldoTokens), 0) FROM Cliente c WHERE c.ativo = true")
    Double sumSaldoTokensAtivos();
}