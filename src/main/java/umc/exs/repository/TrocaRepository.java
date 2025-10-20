package umc.exs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.exs.model.compras.Troca;

public interface TrocaRepository extends JpaRepository<Troca, Long> {
    
}