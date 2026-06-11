package umc.exs.dto.response.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta do dashboard administrativo com métricas consolidadas de clientes, livros, tokens e PIX.
 * Inclui dados históricos dos últimos 12 meses para alimentar os gráficos de cadastros, vendas e postagens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // ── Cards ──────────────────────────────
    private long totalClientes;
    private long totalLivros;
    private long totalVisitas;
    private long totalAdquiridos;
    private double tokensDisponibilizados;
    private double tokensUtilizados;
    private long pixGerados;
    private long pixConvertidos;

    // ── Gráficos (12 meses, mais antigo → mais recente) ──
    private List<String> rotulos; // ex: ["Abr", "Mai", ..., "Mar"]
    private List<Long> clientesPorMes; // novos cadastros por mês
    private List<Long> vendasPorMes; // pedidos concluídos por mês
    private List<Long> postagensPorMes; // livros anunciados por mês
}
