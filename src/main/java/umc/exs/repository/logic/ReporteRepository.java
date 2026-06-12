package umc.exs.repository.logic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.logic.Reporte;

/**
 * Gerencia os reportes e denúncias dos usuários no banco, com suporte a
 * listagem e contagem de não lidos para o badge do painel admin.
 */
@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    /** Lista todos os reportes do mais recente ao mais antigo. */
    List<Reporte> findAllByOrderByDataCriacaoDesc();

    /** Conta os reportes ainda não lidos pelo admin, usado no badge de pendências. */
    long countByLidoFalse();

    /** Lista apenas os reportes não lidos ordenados por data. */
    List<Reporte> findByLidoFalseOrderByDataCriacaoDesc();
}
