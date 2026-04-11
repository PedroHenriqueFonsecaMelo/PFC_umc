/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package umc.exs.repository.negocios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import umc.exs.model.entidades.foundation.Transacao;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByClienteIdOrderByDataHoraDesc(Long clienteId);

    public Transacao findByPagamentoId(String pagamentoId);

    /**
     * Soma de tokens de todas as transações confirmadas (tokens disponibilizados).
     */
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t WHERE t.status = :status")
    Double sumValorByStatus(@Param("status") String status);
}
