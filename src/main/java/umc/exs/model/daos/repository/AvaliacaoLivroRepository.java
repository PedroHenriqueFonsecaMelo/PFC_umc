package umc.exs.model.daos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.AvaliacaoLivro;

public interface AvaliacaoLivroRepository extends JpaRepository<AvaliacaoLivro, Long> {
    
    // Find all reviews for a specific book by ISBN
    List<AvaliacaoLivro> findByIsbn(String isbn);
    
    // Find a specific review by ID
    @SuppressWarnings("null")
    Optional<AvaliacaoLivro> findById(Long id);
    
    // Check if user already reviewed a specific book
    boolean existsByIsbnAndAvaliadorId(String isbn, Long avaliadorId);
}

