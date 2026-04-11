package umc.exs.repository.livro;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.exs.model.entidades.livro.AvaliacaoLivro;

@Repository
public interface AvaliacaoLivroRepository extends JpaRepository<AvaliacaoLivro, Long> {

    // Busca avaliações por Obra
    List<AvaliacaoLivro> findByObraIdOrderByDataAvaliacaoDesc(Long obraId);

    // 1. Resolve o erro: existsByObraIdAndAvaliadorId
    boolean existsByObraIdAndAvaliadorId(Long obraId, Long avaliadorId);

    // 2. Resolve o erro: getAverageRatingByObraId
    @Query("SELECT AVG(a.nota) FROM AvaliacaoLivro a WHERE a.obra.id = :obraId")
    Double getAverageRatingByObraId(@Param("obraId") Long obraId);

    // Busca por ISBN (caso precise no Controller)
    List<AvaliacaoLivro> findByIsbnOriginalNoAtoOrderByDataAvaliacaoDesc(String isbn);
    
    List<AvaliacaoLivro> findAllByTituloLivroIgnoreCaseOrderByDataAvaliacaoDesc(String tituloLivro);
    
    List<AvaliacaoLivro> findAllByTituloLivroIgnoreCaseAndAutorLivroIgnoreCaseOrderByDataAvaliacaoDesc(String titulo, String autor);
}