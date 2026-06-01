package umc.exs.service.cliente.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.exs.dto.response.cliente.ClienteListaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.*;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClienteAdminService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final TransacaoRepository transacaoRepository;
    private final CupomUsoRepository cupomUsoRepository;
    private final TopicoForumRepository topicoForumRepository;
    private final ListaDesejosRepository listaDesejosRepository;
    private final LoteRepository loteRepository;
    private final LivroRepository livroRepository;
    private final SolicitacaoCancelamentoRepository cancelamentoRepository;
    private final LogAuditoriaService logAuditoria;

    @Transactional(readOnly = true)
    public List<ClienteListaResponse> listarClientes() {

        List<Cliente> clientes = clienteRepository.findAll();
        Map<Long, long[]> statsMap = mapearStatsPedidos();

        logAuditoria.registrarLog(
                AcaoAuditoria.CLIENTE_LISTAGEM.name(),
                null,
                null,
                "Listagem de clientes consultada");

        return clientes.stream()
                .map(c -> mapToClienteListaResponse(c, statsMap))
                .toList();
    }

    private Map<Long, long[]> mapearStatsPedidos() {
        List<Object[]> stats = pedidoRepository.statsGroupedByComprador();
        Map<Long, long[]> map = new HashMap<>();

        for (Object[] row : stats) {
            Long clienteId = (Long) row[0];
            long count = ((Number) row[1]).longValue();
            double gasto = ((Number) row[2]).doubleValue();
            map.put(clienteId, new long[]{count, Double.doubleToLongBits(gasto)});
        }

        return map;
    }

    private ClienteListaResponse mapToClienteListaResponse(Cliente c, Map<Long, long[]> statsMap) {

        long[] s = statsMap.getOrDefault(c.getId(), new long[]{0L, Double.doubleToLongBits(0.0)});
        long totalCompras = s[0];
        double totalGasto = Double.longBitsToDouble(s[1]);

        return new ClienteListaResponse(
                c.getId(),
                c.getNome(),
                c.getEmail(),
                c.getDataCriacao(),
                c.getSaldoTokens() != null ? c.getSaldoTokens() : 0.0,
                calcularNivel(totalGasto),
                totalCompras,
                totalGasto,
                c.isAtivo());
    }

    @Transactional(readOnly = true)
    public ClientePerfilResponse getPerfilCliente(Long clienteId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + clienteId));

        List<Pedido> pedidos = pedidoRepository.findByCompradorIdOrderByDataCompraDesc(clienteId);

        Double totalGasto = pedidoRepository.sumGastoByClienteId(clienteId);
        totalGasto = totalGasto != null ? totalGasto : 0.0;

        Double totalRecarregado = transacaoRepository.sumValorConfirmadoByClienteId(clienteId);
        totalRecarregado = totalRecarregado != null ? totalRecarregado : 0.0;

        long cuponsUsados = cupomUsoRepository.countByClienteId(clienteId);
        long totalCancelamentos = cancelamentoRepository.countByClienteId(clienteId);

        long livrosVendidos = livroRepository.countByVendedorIdAndAprovadoTrue(clienteId);
        long livrosRejeitados = livroRepository.countRejeitadosByVendedorId(clienteId);

        long lotesEnviados = loteRepository.countByClienteIdAndStatusIn(
                clienteId,
                List.of(Lote.LoteStatus.PARCIAL_APROVADO, Lote.LoteStatus.TOTAL_APROVADO));

        long topicosForum = topicoForumRepository.countByAutorId(clienteId);
        long listaDesejos = listaDesejosRepository.countByClienteId(clienteId);

        logAuditoria.registrarLog(
                AcaoAuditoria.CLIENTE_PERFIL_VISUALIZADO.name(),
                clienteId,
                cliente.getEmail(),
                "Perfil do cliente consultado");

        return ClientePerfilResponse.builder()
                .id(cliente.getId())
                .nome(cliente.getNome())
                .email(cliente.getEmail())
                .cpfMascarado(mascararCpf(cliente.getCpf()))
                .dataNascimento(cliente.getDatanasc())
                .dataCadastro(cliente.getDataCriacao())
                .ativo(cliente.isAtivo())
                .nivel(calcularNivel(totalGasto))
                .saldoTokens(cliente.getSaldoTokens() != null ? cliente.getSaldoTokens() : 0.0)
                .totalGasto(totalGasto)
                .totalRecarregado(totalRecarregado)
                .quantidadeCuponsUsados(cuponsUsados)
                .totalPedidos(pedidos.size())
                .totalCancelamentos(totalCancelamentos)
                .pedidos(pedidos.stream().limit(20).map(this::mapToPedidoResumoDTO).toList())
                .totalLivrosVendidos(livrosVendidos)
                .totalLotesEnviados(lotesEnviados)
                .totalLivrosRejeitados(livrosRejeitados)
                .totalTopicosForum(topicosForum)
                .totalListaDesejos(listaDesejos)
                .build();
    }

    private ClientePerfilResponse.PedidoResumoDTO mapToPedidoResumoDTO(Pedido p) {
        return new ClientePerfilResponse.PedidoResumoDTO(
                p.getId(),
                p.getTituloLivro(),
                p.getAutorLivro(),
                p.getPrecoLivro(),
                p.getStatusEnvio() != null ? p.getStatusEnvio().name() : "—",
                p.getDataCompra(),
                p.getCodigoRastreio());
    }

    private String calcularNivel(double totalGasto) {
        if (totalGasto >= 500) return "Platina";
        if (totalGasto >= 200) return "Ouro";
        if (totalGasto >= 50) return "Prata";
        return "Bronze";
    }

    private String mascararCpf(String cpf) {
        if (cpf == null) return "***.***.***-**";
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() != 11) return "***.***.***-**";
        return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
    }
}