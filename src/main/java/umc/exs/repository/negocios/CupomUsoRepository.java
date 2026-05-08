package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.CupomUso;

@Repository
public interface CupomUsoRepository extends JpaRepository<CupomUso, Long> {

    /** Verifica se um cliente já usou este cupom (impede uso duplicado). */
    boolean existsByCupomIdAndClienteId(Long cupomId, Long clienteId);

    /** Total de vezes que um cupom foi utilizado. */
    long countByCupomId(Long cupomId);
}
