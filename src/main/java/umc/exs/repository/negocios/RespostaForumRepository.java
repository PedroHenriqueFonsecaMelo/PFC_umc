package umc.exs.repository.negocios;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;

/**
 * Gerencia as respostas do fórum no banco, com suporte a curtidas e marcação
 * de melhor resposta.
 */
@Repository
public interface RespostaForumRepository extends JpaRepository<RespostaForum, Long> {

        /** Busca a resposta marcada como melhor em um tópico, usada ao alternar a marcação. */
        Optional<RespostaForum> findByTopicoAndMelhorRespostaTrue(TopicoForum topico);

        /** Conta o total de respostas de um tópico para exibição na listagem. */
        long countByTopicoId(Long topicoId);

        /**
         * Retorna os IDs das respostas curtidas por um cliente em um tópico específico,
         * usado para destacar curtidas já dadas na interface.
         */
        @Query("""
                        SELECT r.id FROM RespostaForum r JOIN r.curtidoresIds c
                        WHERE r.topico.id = :topicoId AND c = :clienteId
                        """)
        Set<Long> findRespostaIdsLikedByClienteInTopico(
                        @Param("topicoId") Long topicoId,
                        @Param("clienteId") Long clienteId);
}
