package umc.exs.DTOs.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricasDTO {

    // ── Cards ──────────────────────────────
    private long totalClientes;
    private long totalLivros;
    private long totalVisitas;
    private long totalAdquiridos;
    private double tokensDisponibilizados;
    private double tokensUtilizados;

    // ── Gráficos (12 meses, mais antigo → mais recente) ──
    private List<String> rotulos; // ex: ["Abr", "Mai", ..., "Mar"]
    private List<Long> clientesPorMes; // novos cadastros por mês
    private List<Long> vendasPorMes; // pedidos concluídos por mês
    private List<Long> postagensPorMes; // livros anunciados por mês
}
