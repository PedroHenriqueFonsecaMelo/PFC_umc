package umc.exs.repository.logic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.logic.Reporte;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    List<Reporte> findAllByOrderByDataCriacaoDesc();

    long countByLidoFalse();

    List<Reporte> findByLidoFalseOrderByDataCriacaoDesc();
}
