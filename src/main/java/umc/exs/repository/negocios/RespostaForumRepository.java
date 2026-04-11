package umc.exs.repository.negocios;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;

@Repository
public interface RespostaForumRepository extends JpaRepository<RespostaForum, Long> {

        Optional<RespostaForum> findByTopicoAndMelhorRespostaTrue(TopicoForum topico);

        long countByTopicoId(Long topicoId);

        @Query("""
                        SELECT r.id FROM RespostaForum r JOIN r.curtidoresIds c
                        WHERE r.topico.id = :topicoId AND c = :clienteId
                        """)
        Set<Long> findRespostaIdsLikedByClienteInTopico(
                        @Param("topicoId") Long topicoId,
                        @Param("clienteId") Long clienteId);
}
