package umc.exs.repository.negocios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.Cupom;

@Repository
public interface CupomRepository extends JpaRepository<Cupom, Long> {

    Optional<Cupom> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Cupom> findByClienteIdAndUsadoFalse(Long clienteId);

    /** Cupons não usados e não expirados de um cliente (para listar disponíveis). */
    List<Cupom> findByClienteIdAndUsadoFalseAndExpiracaoAfter(Long clienteId, LocalDateTime agora);

    /** Cupons públicos válidos (cliente = null). */
    List<Cupom> findByClienteIsNullAndUsadoFalseAndExpiracaoAfter(LocalDateTime agora);

    /** Cupons não usados já expirados (para limpeza diária). */
    List<Cupom> findByUsadoFalseAndExpiracaoBefore(LocalDateTime agora);

    /** Cupons a vencer em um intervalo de tempo (aviso de 7 dias). */
    @Query("SELECT c FROM Cupom c WHERE c.usado = false AND c.expiracao BETWEEN :inicio AND :fim")
    List<Cupom> findByUsadoFalseAndExpiracaoBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
