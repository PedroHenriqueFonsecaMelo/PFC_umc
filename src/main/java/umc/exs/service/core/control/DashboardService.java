package umc.exs.service.core.control;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.admin.DashboardMetricasDTO;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.VisitaSiteRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.TransacaoRepository;
import umc.exs.repository.usuario.ClienteRepository;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final ClienteRepository clienteRepository;
        private final LivroRepository livroRepository;
        private final PedidoRepository pedidoRepository;
        private final TransacaoRepository transacaoRepository;
        private final VisitaSiteRepository visitaSiteRepository;

        @SuppressWarnings("deprecation")
        private static final Locale PT_BR = new Locale("pt", "BR");

        @Transactional(readOnly = true)
        public DashboardMetricasDTO getMetricas() {

                // ── Cards ────────────────────────────────────────────────
                long totalClientes = clienteRepository.count();
                long totalLivros = livroRepository.count();
                long totalVisitas = visitaSiteRepository.sumTotalVisitas();
                long totalAdquiridos = pedidoRepository.count();

                Double tokensDisp = transacaoRepository.sumValorByStatus("CONFIRMADO");
                Double tokensUtil = pedidoRepository.sumTokensUtilizados();
                double tokensDisponibilizados = tokensDisp != null ? tokensDisp : 0.0;
                double tokensUtilizados = tokensUtil != null ? tokensUtil : 0.0;

                // ── Gráficos: últimos 12 meses ────────────────────────────
                // Início = primeiro dia do mês de 11 meses atrás, à meia-noite
                LocalDateTime inicio = YearMonth.now().minusMonths(11)
                                .atDay(1).atStartOfDay();

                // Busca registros dos últimos 12 meses
                var clientes = clienteRepository.findByDataCriacaoAfter(inicio);
                var pedidos = pedidoRepository.findByDataCompraAfter(inicio);
                var livros = livroRepository.findByDataAnuncioAfter(inicio);

                // Agrupa por YearMonth em Java (compatível com SQLite e PostgreSQL)
                Map<YearMonth, Long> clientesMap = clientes.stream()
                                .collect(Collectors.groupingBy(
                                                c -> YearMonth.from(c.getDataCriacao()), Collectors.counting()));

                Map<YearMonth, Long> vendasMap = pedidos.stream()
                                .collect(Collectors.groupingBy(
                                                p -> YearMonth.from(p.getDataCompra()), Collectors.counting()));

                Map<YearMonth, Long> postagensMap = livros.stream()
                                .filter(l -> l.getDataAnuncio() != null)
                                .collect(Collectors.groupingBy(
                                                l -> YearMonth.from(l.getDataAnuncio()), Collectors.counting()));

                // Monta arrays de 12 posições (mês mais antigo → mais recente)
                List<String> rotulos = new ArrayList<>();
                List<Long> clientesMensais = new ArrayList<>();
                List<Long> vendasMensais = new ArrayList<>();
                List<Long> postagensMensais = new ArrayList<>();

                for (int i = 11; i >= 0; i--) {
                        YearMonth ym = YearMonth.now().minusMonths(i);
                        rotulos.add(ym.getMonth().getDisplayName(TextStyle.SHORT, PT_BR));
                        clientesMensais.add(clientesMap.getOrDefault(ym, 0L));
                        vendasMensais.add(vendasMap.getOrDefault(ym, 0L));
                        postagensMensais.add(postagensMap.getOrDefault(ym, 0L));
                }

                return DashboardMetricasDTO.builder()
                                .totalClientes(totalClientes)
                                .totalLivros(totalLivros)
                                .totalVisitas(totalVisitas)
                                .totalAdquiridos(totalAdquiridos)
                                .tokensDisponibilizados(tokensDisponibilizados)
                                .tokensUtilizados(tokensUtilizados)
                                .rotulos(rotulos)
                                .clientesPorMes(clientesMensais)
                                .vendasPorMes(vendasMensais)
                                .postagensPorMes(postagensMensais)
                                .build();
        }
}
