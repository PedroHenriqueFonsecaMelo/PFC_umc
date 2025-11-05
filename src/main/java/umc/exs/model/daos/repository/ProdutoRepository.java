package umc.exs.model.daos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    
}
