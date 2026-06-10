package umc.exs.service.core.dashboard;

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
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.response.admin.DashboardResponse;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.VisitaSiteRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.TransacaoRepository;
import umc.exs.repository.usuario.ClienteRepository;

@Slf4j
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
        public DashboardResponse getMetricas() {

                long inicioExecucao = System.currentTimeMillis();

                try {

                        long totalClientes = clienteRepository.count();
                        long totalLivros = livroRepository.count();

                        Long visitasRaw = visitaSiteRepository.sumTotalVisitas();
                        long totalVisitas = visitasRaw != null ? visitasRaw : 0L;

                        long totalAdquiridos = pedidoRepository.count();

                        Double tokensDisp = clienteRepository.sumSaldoTokensAtivos();
                        Double tokensUtil = pedidoRepository.sumTokensUtilizados();

                        double tokensDisponibilizados = tokensDisp != null ? tokensDisp : 0.0;
                        double tokensUtilizados = tokensUtil != null ? tokensUtil : 0.0;

                        long pixGerados = transacaoRepository.count();
                        long pixConvertidos = transacaoRepository.countByStatus("CONFIRMADO");

                        YearMonth base = YearMonth.now();

                        LocalDateTime inicio = base.minusMonths(11)
                                        .atDay(1)
                                        .atStartOfDay();

                        var clientes = clienteRepository.findByDataCriacaoAfter(inicio);
                        var datasCompra = pedidoRepository.findDataCompraAfterProjection(inicio);
                        var datasAnuncio = livroRepository.findDataAnuncioAfterProjection(inicio);

                        // =========================
                        // MAPEAMENTOS SEGUROS
                        // =========================

                        Map<YearMonth, Long> clientesMap = clientes.stream()
                                        .collect(Collectors.groupingBy(
                                                        c -> YearMonth.from(c.getDataCriacao()),
                                                        Collectors.counting()));

                        Map<YearMonth, Long> vendasMap = datasCompra.stream()
                                        .filter(d -> d != null)
                                        .collect(Collectors.groupingBy(
                                                        d -> YearMonth.from(d),
                                                        Collectors.counting()));

                        Map<YearMonth, Long> postagensMap = datasAnuncio.stream()
                                        .filter(d -> d != null)
                                        .collect(Collectors.groupingBy(
                                                        d -> YearMonth.from(d),
                                                        Collectors.counting()));

                        List<String> rotulos = new ArrayList<>();
                        List<Long> clientesMensais = new ArrayList<>();
                        List<Long> vendasMensais = new ArrayList<>();
                        List<Long> postagensMensais = new ArrayList<>();

                        for (int i = 11; i >= 0; i--) {

                                YearMonth ym = base.minusMonths(i);

                                rotulos.add(
                                                ym.getMonth().getDisplayName(TextStyle.SHORT, PT_BR));

                                clientesMensais.add(clientesMap.getOrDefault(ym, 0L));
                                vendasMensais.add(vendasMap.getOrDefault(ym, 0L));
                                postagensMensais.add(postagensMap.getOrDefault(ym, 0L));
                        }

                        DashboardResponse response = DashboardResponse.builder()
                                        .totalClientes(totalClientes)
                                        .totalLivros(totalLivros)
                                        .totalVisitas(totalVisitas)
                                        .totalAdquiridos(totalAdquiridos)
                                        .tokensDisponibilizados(tokensDisponibilizados)
                                        .tokensUtilizados(tokensUtilizados)
                                        .pixGerados(pixGerados)
                                        .pixConvertidos(pixConvertidos)
                                        .rotulos(rotulos)
                                        .clientesPorMes(clientesMensais)
                                        .vendasPorMes(vendasMensais)
                                        .postagensPorMes(postagensMensais)
                                        .build();

                        

                        log.info(
                                        "DASHBOARD_METRICAS_CONSULTADAS clientes={} livros={} visitas={} pedidos={} pixGerados={} pixConvertidos={} tempoMs={}",
                                        totalClientes,
                                        totalLivros,
                                        totalVisitas,
                                        totalAdquiridos,
                                        pixGerados,
                                        pixConvertidos,
                                        System.currentTimeMillis() - inicioExecucao);

                        return response;

                } catch (Exception e) {


                        log.error("DASHBOARD_METRICAS_ERRO", e);

                        throw e;
                }
        }
}