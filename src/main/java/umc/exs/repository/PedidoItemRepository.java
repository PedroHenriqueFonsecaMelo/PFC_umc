package umc.exs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.compras.PedidoItem;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {
    List<PedidoItem> findByPedidoId(Long pedidoId);
    List<PedidoItem> findByPedidoIdIn(List<Long> pedidoIds);
}