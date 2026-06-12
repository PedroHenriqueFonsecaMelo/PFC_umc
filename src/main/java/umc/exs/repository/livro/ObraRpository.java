package umc.exs.repository.livro;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.livro.Obra;

/**
 * Gerencia as obras literárias canônicas no banco, usadas para agrupar edições
 * e avaliações do mesmo título independente do ISBN.
 */
public interface ObraRpository extends JpaRepository<Obra, Long> {

    /** Busca uma obra pelo título ignorando maiúsculas e minúsculas. */
    Optional<Obra> findByTituloIgnoreCase(String titulo);

    /** Busca uma obra pela combinação exata de título e autor para evitar duplicatas. */
    Optional<Obra> findByTituloAndAutor(String titulo, String autor);

}
