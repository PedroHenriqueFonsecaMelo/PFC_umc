package umc.exs.model.daos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.compras.Troca;

public interface TrocaRepository extends JpaRepository<Troca, Long> {
    
}