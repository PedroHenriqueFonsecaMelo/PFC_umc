package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.exs.model.entidades.foundation.ListaDesejos;

import java.util.List;
import java.util.Optional;

public interface ListaDesejosRepository extends JpaRepository<ListaDesejos, Long> {

    List<ListaDesejos> findByClienteId(Long clienteId);

    List<ListaDesejos> findByIsbn(String isbn);

    Optional<ListaDesejos> findByClienteIdAndIsbn(Long clienteId, String isbn);

    boolean existsByClienteIdAndIsbn(Long clienteId, String isbn);
}
