package umc.exs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import umc.exs.model.compras.Pedido;
import umc.exs.model.compras.PedidoItem;
import umc.exs.repository.PedidoItemRepository;
import umc.exs.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoItemRepository pedidoItemRepository;

    public Map<String, Object> computeSalesStats(String since, String until) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (since != null && !since.isBlank()) start = LocalDateTime.parse(since);
            if (until != null && !until.isBlank()) end = LocalDateTime.parse(until);
        } catch (DateTimeParseException e) {
            // ignore invalid parse, treat as null (full range)
        }

        List<Pedido> pedidos;
        if (start != null && end != null) {
            pedidos = pedidoRepository.findByDataBetween(start, end);
        } else {
            pedidos = pedidoRepository.findAll();
        }

        int totalOrders = pedidos.size();

        // compute revenue: prefer pedido.total when available, otherwise sum items
        double totalRevenue = 0.0;
        List<Long> pedidoIds = pedidos.stream()
                .map(Pedido::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<PedidoItem> items = pedidoIds.isEmpty()
                ? Collections.emptyList()
                : pedidoItemRepository.findByPedidoIdIn(pedidoIds);

        // sum revenue using pedido.total when present
        double pedidoTotals = pedidos.stream()
                .filter(p -> p.getTotal() != null)
                .mapToDouble(Pedido::getTotal)
                .sum();

        items = items.stream().filter(it -> Objects.nonNull(it.getPrecoUnitario()))
                .filter(it -> Objects.nonNull(it.getQuantidade())).toList();

        // sum remaining via items for pedidos without total or to be safe sum items too
        double itemsSum = items.stream()
                .mapToDouble(it -> (it.getPrecoUnitario() == null ? 0.0 : it.getPrecoUnitario()) * (it.getQuantidade() == null ? 0 : it.getQuantidade()))
                .sum();

        // prefer the sum of item values as canonical revenue (safer)
        totalRevenue = Math.max(pedidoTotals, itemsSum);

        // aggregate products sold
        Map<Long, ProductStats> map = new HashMap<>();
        for (PedidoItem it : items) {
            Long pid = it.getProdutoId();
            if (pid == null) continue;
            ProductStats ps = map.computeIfAbsent(pid, k -> new ProductStats(pid, it.getProdutoTitulo(), 0L, 0.0));
            long qty = it.getQuantidade() == null ? 0L : it.getQuantidade();
            double rev = (it.getPrecoUnitario() == null ? 0.0 : it.getPrecoUnitario()) * qty;
            ps.qty += qty;
            ps.revenue += rev;
        }

        List<Map<String, Object>> productsSold = map.values().stream()
                .map(ps -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", ps.productId);
                    m.put("titulo", ps.titulo);
                    m.put("quantity", ps.qty);
                    m.put("revenue", ps.revenue);
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("totalRevenue", totalRevenue);
        result.put("productsSold", productsSold);
        // privacy: do not include customer identifiers or personal data

        return result;
    }

    private static class ProductStats {
        Long productId;
        String titulo;
        long qty;
        double revenue;

        ProductStats(Long productId, String titulo, long qty, double revenue) {
            this.productId = productId;
            this.titulo = titulo;
            this.qty = qty;
            this.revenue = revenue;
        }
    }
}