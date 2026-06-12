package umc.exs.repository.usuario;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.social.PontuacaoUsuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório responsável por gerenciar as pontuações de XP dos clientes no banco de dados.
 * Oferece suporte a ranking global, expiração de pontos e cálculo de posição no ranking,
 * possibilitando consultas otimizadas para o sistema de gamificação da plataforma.
 */
@Repository
public interface PontuacaoUsuarioRepository extends JpaRepository<PontuacaoUsuario, Long> {

       /**
        * Busca a pontuação de um cliente pelo seu ID.
        */
       Optional<PontuacaoUsuario> findByClienteId(Long clienteId);

       /**
        * Busca a pontuação de um cliente pelo seu endereço de e-mail.
        */
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

       /**
        * Lista todas as pontuações com o cliente carregado via JOIN FETCH,
        * evitando o problema N+1 ao acessar os dados do cliente associado.
        */
       @Query("SELECT p FROM PontuacaoUsuario p JOIN FETCH p.cliente")
       List<PontuacaoUsuario> findAllWithCliente();

       /**
        * Pontuações com XP > 0 cujo prazo de expiração já passou (para limpeza).
        */
       @Query("SELECT p FROM PontuacaoUsuario p JOIN FETCH p.cliente " +
                     "WHERE p.xpTotal > 0 AND p.dataExpiracao IS NOT NULL AND p.dataExpiracao < :agora")
       List<PontuacaoUsuario> findExpirados(@Param("agora") LocalDateTime agora);

       /**
        * Pontuações com XP > 0 que vencem entre agora e um limite (aviso prévio).
        */
       @Query("SELECT p FROM PontuacaoUsuario p JOIN FETCH p.cliente " +
                     "WHERE p.xpTotal > 0 AND p.dataExpiracao IS NOT NULL " +
                     "AND p.dataExpiracao BETWEEN :inicio AND :fim")
       List<PontuacaoUsuario> findAVencer(
                     @Param("inicio") LocalDateTime inicio,
                     @Param("fim") LocalDateTime fim);

       /**
        * Lista pontuações cuja última atualização ocorreu antes da data informada.
        * Utilizado pelo scheduler de decaimento de XP para identificar registros
        * que ficaram inativos e devem ter seus pontos reduzidos.
        */
       List<PontuacaoUsuario> findAllByUltimaAtualizacaoBefore(LocalDateTime data);

       /**
        * Pontuações cuja última atualização ficou entre duas datas (janela de alerta
        * de XP).
        */
       List<PontuacaoUsuario> findAllByUltimaAtualizacaoBetween(LocalDateTime inicio, LocalDateTime fim);

       /** Ranking público — todos os tempos, top N por XP. */
       @Query("SELECT p FROM PontuacaoUsuario p JOIN FETCH p.cliente ORDER BY p.xpTotal DESC")
       List<PontuacaoUsuario> findRankingTodos(Pageable pageable);

       /** Ranking público — filtrado a partir de uma data (mês/ano). */
       @Query("SELECT p FROM PontuacaoUsuario p JOIN FETCH p.cliente " +
                     "WHERE p.ultimaAtualizacao >= :desde ORDER BY p.xpTotal DESC")
       List<PontuacaoUsuario> findRankingDesde(@Param("desde") LocalDateTime desde, Pageable pageable);
}
