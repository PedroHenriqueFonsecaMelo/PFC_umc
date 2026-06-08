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
     * Retorna null quando não há registros; o service trata com orElse(0.0).
     */
    @Query("SELECT SUM(t.valor) FROM Transacao t WHERE t.status = :status")
    Double sumValorByStatus(@Param("status") String status);

    long countByStatus(String status);

    long count();

    /** Soma de tokens recarregados confirmados por um cliente. */
    @Query("SELECT COALESCE(SUM(t.valor), 0.0) FROM Transacao t WHERE t.cliente.id = :clienteId AND t.status = 'CONFIRMADO'")
    Double sumValorConfirmadoByClienteId(@Param("clienteId") Long clienteId);

    List<Transacao> findByClienteIdAndStatusOrderByDataHoraDesc(Long clienteId, String statusConcluido);
}
