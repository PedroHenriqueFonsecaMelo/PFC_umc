package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.Cartao;

public interface  CartaoRepository extends JpaRepository<Cartao, Long>{
    
}
