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

    boolean existsByIsbnOrTitulo(String isbn, String titulo);

    boolean existsByIsbn(String isbn);

    long countByClienteId(Long clienteId);

    // Valida se o cliente já adicionou EXATAMENTE essa edição do Google Books
    boolean existsByClienteIdAndGoogleBookId(Long clienteId, String googleBookId);

    // ALTERNATIVA: Valida se o cliente já tem ESSA OBRA (por título e autor) para evitar "O Hobbit" duplicado
    boolean existsByClienteIdAndTituloIgnoreCaseAndAutorIgnoreCase(Long clienteId, String titulo, String autor);

    boolean existsByClienteIdAndOpenLibraryWorkId(Long id, String openLibraryWorkId);
}
