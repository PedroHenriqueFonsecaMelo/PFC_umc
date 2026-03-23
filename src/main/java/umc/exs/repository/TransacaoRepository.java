/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package umc.exs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.Transacao;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByClienteIdOrderByDataHoraDesc(Long clienteId);

    public Transacao findByPagamentoId(String pagamentoId);
}
