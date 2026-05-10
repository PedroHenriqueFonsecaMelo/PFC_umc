package umc.exs.repository.livro;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.livro.Obra;

public interface ObraRpository extends JpaRepository<Obra, Long> {

    Optional<Obra> findByTituloIgnoreCase(String titulo);

    Optional<Obra> findByTituloAndAutor(String titulo, String autor);

}
