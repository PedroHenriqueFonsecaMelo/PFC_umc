package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.foundation.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    
}
