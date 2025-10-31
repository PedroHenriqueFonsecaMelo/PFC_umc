package umc.exs.backstage.service;

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
            if (since != null && !since.isBlank())
                start = LocalDateTime.parse(since);
            if (until != null && !until.isBlank())
                end = LocalDateTime.parse(until);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use ISO_LOCAL_DATE_TIME format.");
        }

        List<Pedido> pedidos;
        if (start != null && end != null) {
            pedidos = pedidoRepository.findByDataBetween(start, end);
        } else {
            pedidos = pedidoRepository.findAll();
        }

        int totalOrders = pedidos.size();

        List<Long> pedidoIds = pedidos.stream()
                .map(Pedido::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<PedidoItem> items = pedidoIds.isEmpty()
                ? Collections.emptyList()
                : pedidoItemRepository.findByPedidoIdIn(pedidoIds);

        double pedidoTotals = pedidos.stream()
                .filter(p -> p.getTotal() != null)
                .mapToDouble(Pedido::getTotal)
                .sum();

        items = items.stream().filter(it -> Objects.nonNull(it.getPrecoUnitario()))
                .filter(it -> Objects.nonNull(it.getQuantidade())).toList();

        double itemsSum = items.stream()
                .mapToDouble(it -> {
                    double preco = Optional.ofNullable(it.getPrecoUnitario()).orElse(0.0);
                    int qtd = Optional.ofNullable(it.getQuantidade()).orElse(0);
                    return preco * qtd;
                })
                .sum();

        double totalRevenue = Math.max(pedidoTotals, itemsSum);

        Map<Long, ProductStats> map = new HashMap<>();
        for (PedidoItem it : items) {
            Long pid = it.getProdutoId();
            if (pid == null)
                continue;
            ProductStats ps = map.computeIfAbsent(pid, k -> new ProductStats(pid, it.getProdutoTitulo(), 0L, 0.0));
            Integer qtdObj = it.getQuantidade();
            Double precoObj = it.getPrecoUnitario();

            long qty = (qtdObj != null) ? qtdObj.longValue() : 0L;
            double preco = (precoObj != null) ? precoObj : 0.0;
            double rev = preco * qty;

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