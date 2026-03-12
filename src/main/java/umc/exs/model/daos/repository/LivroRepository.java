package umc.exs.model.daos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.LivroAnuncio;

public interface LivroRepository extends JpaRepository<LivroAnuncio, Long> {

    // Buscar livros APROVADOS (para a vitrine)
    List<LivroAnuncio> findByAprovadoTrue();

    // Buscar livros PENDENTES (para o admin)
    List<LivroAnuncio> findByAprovadoFalse();

    // Buscar livro por ID e que esteja APROVADO
    Optional<LivroAnuncio> findByIdAndAprovadoTrue(Long id);
}
