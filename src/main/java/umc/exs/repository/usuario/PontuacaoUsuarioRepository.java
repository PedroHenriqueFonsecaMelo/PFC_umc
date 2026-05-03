package umc.exs.repository.usuario;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.social.PontuacaoUsuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface PontuacaoUsuarioRepository extends JpaRepository<PontuacaoUsuario, Long> {

    Optional<PontuacaoUsuario> findByClienteId(Long clienteId);

    Optional<PontuacaoUsuario> findByClienteEmail(String email);

    /**
     * Top N usuários por XP — usa Pageable para ser compatível com
     * SQLite (dev) e PostgreSQL (produção). Evita LIMIT hardcoded no JPQL.
     * Uso: findTopByOrderByXpTotalDesc(PageRequest.of(0, 5))
     */
    @Query("SELECT p FROM PontuacaoUsuario p ORDER BY p.xpTotal DESC")
    List<PontuacaoUsuario> findTopByOrderByXpTotalDesc(Pageable pageable);

    /**
     * Conta quantos usuários têm XP estritamente maior que o valor informado.
     * Posição do usuário = countByXpTotalGreaterThan(xpDoUsuario) + 1.
     */
    @Query("SELECT COUNT(p) FROM PontuacaoUsuario p WHERE p.xpTotal > :xp")
    long countByXpTotalGreaterThan(@Param("xp") int xp);

    @Query("SELECT p FROM PontuacaoUsuario p JOIN FETCH p.cliente")
    List<PontuacaoUsuario> findAllWithCliente();
}
