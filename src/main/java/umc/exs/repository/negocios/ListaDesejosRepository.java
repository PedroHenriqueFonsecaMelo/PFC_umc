package umc.exs.repository.negocios;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.exs.model.entidades.foundation.ListaDesejos;

import java.util.List;
import java.util.Optional;

/**
 * Gerencia a lista de desejos dos clientes no banco, com busca por cliente e
 * ISBN.
 */
public interface ListaDesejosRepository extends JpaRepository<ListaDesejos, Long> {

    /** Lista todos os desejos de um cliente. */
    List<ListaDesejos> findByClienteId(Long clienteId);

    /** Lista todos os clientes interessados em um ISBN específico, usado para notificações de disponibilidade. */
    List<ListaDesejos> findByIsbn(String isbn);

    /** Busca um desejo específico de um cliente por ISBN. */
    Optional<ListaDesejos> findByClienteIdAndIsbn(Long clienteId, String isbn);

    /** Verifica se um cliente já tem o ISBN na lista de desejos para evitar duplicatas. */
    boolean existsByClienteIdAndIsbn(Long clienteId, String isbn);

    /** Conta o total de desejos de um cliente. */
    long countByClienteId(Long clienteId);
}
