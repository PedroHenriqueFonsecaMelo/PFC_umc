package umc.exs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.AvaliacaoLivro;

public interface AvaliacaoLivroRepository extends JpaRepository<AvaliacaoLivro, Long> {

    /**
     * Busca todas avaliações por ISBN livro específico.
     * 
     * @param isbn código livro
     * @return lista AvaliacaoLivro
     */
    List<AvaliacaoLivro> findByIsbn(String isbn);

    /**
     * Busca avaliação específica por ID.
     * 
     * @param id avaliação
     * @return Optional AvaliacaoLivro
     */
    @SuppressWarnings("null")
    Optional<AvaliacaoLivro> findById(Long id);

    /**
     * Verifica se usuário já avaliou livro específico.
     * Previne avaliações duplicadas.
     * 
     * @param isbn        livro
     * @param avaliadorId cliente
     * @return true se existe
     */
    boolean existsByIsbnAndAvaliadorId(String isbn, Long avaliadorId);
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Repositório JPA avaliações livros.
 * Queries custom: por ISBN, ID, exists usuário-livro.
 * Extends JpaRepository<AvaliacaoLivro, Long>.
 */
