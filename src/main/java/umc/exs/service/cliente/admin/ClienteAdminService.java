package umc.exs.service.cliente.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import umc.exs.dto.response.cliente.ClienteListaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.StatusConta;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.*;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;

import java.time.LocalDateTime;
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
        private final EmailFacade emailFacade;

        @Transactional(readOnly = true)
        public Page<ClienteListaResponse> listarClientes(Pageable pageable) {

                Page<Cliente> paginaClientes = clienteRepository.findAll(pageable);

                if (paginaClientes.isEmpty()) {
                        return Page.empty(pageable);
                }

                List<Long> clienteIds = paginaClientes.stream()
                                .map(Cliente::getId)
                                .toList();

                Map<Long, long[]> statsMap = mapearStatsPedidosFiltrados(clienteIds);

                logAuditoria.registrarLog(
                                AcaoAuditoria.CLIENTE_LISTAGEM.name(),
                                null,
                                null,
                                "Listagem de clientes consultada - Página: " + pageable.getPageNumber());

                return paginaClientes.map(c -> mapToClienteListaResponse(c, statsMap));
        }

        private Map<Long, long[]> mapearStatsPedidosFiltrados(List<Long> clienteIds) {
                // Busca no banco usando a nova query otimizada com o filtro IN
                List<Object[]> stats = pedidoRepository.statsGroupedByCompradorIds(clienteIds);
                Map<Long, long[]> map = new HashMap<>();

                for (Object[] row : stats) {
                        Long clienteId = (Long) row[0];
                        long count = ((Number) row[1]).longValue();
                        double gasto = ((Number) row[2]).doubleValue(); // p.precoLivro
                        map.put(clienteId, new long[] { count, Double.doubleToLongBits(gasto) });
                }

                return map;
        }

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
                        map.put(clienteId, new long[] { count, Double.doubleToLongBits(gasto) });
                }

                return map;
        }

        private ClienteListaResponse mapToClienteListaResponse(Cliente c, Map<Long, long[]> statsMap) {

                long[] s = statsMap.getOrDefault(c.getId(), new long[] { 0L, Double.doubleToLongBits(0.0) });
                long totalCompras = s[0];
                double totalGasto = Double.longBitsToDouble(s[1]);
                long totalCupons = cupomUsoRepository.countByClienteId(c.getId());

                return new ClienteListaResponse(
                                c.getId(),
                                c.getNome(),
                                c.getEmail(),
                                c.getDataCriacao(),
                                c.getSaldoTokens() != null ? c.getSaldoTokens() : 0.0,
                                calcularNivel(totalGasto),
                                totalCompras,
                                totalGasto,
                                totalCupons,
                                c.isAtivo(),
                                c.getStatusConta() != null ? c.getStatusConta() : StatusConta.ATIVO);
        }

        @Transactional(readOnly = true)
        public ClientePerfilResponse getPerfilCliente(Long clienteId) {

                Cliente cliente = clienteRepository.findById(clienteId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Cliente não encontrado: " + clienteId));

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
                                .statusConta(cliente.getStatusConta() != null ? cliente.getStatusConta()
                                                : StatusConta.ATIVO)
                                .suspensaoAte(cliente.getSuspensaoAte())
                                .motivoSuspensao(cliente.getMotivoSuspensao())
                                .dataAcao(cliente.getDataAcao())
                                .adminAcao(cliente.getAdminAcao())
                                .emailNotificadoEm(cliente.getEmailNotificadoEm())
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

        private static final String MOTIVO_PADRAO_SUSPENSAO = "Sua conta na Bibliotroca foi suspensa temporariamente devido a uma violação dos nossos termos de uso. "
                        +
                        "Entre em contato com nosso suporte caso acredite que isso foi um engano.";

        private static final String MOTIVO_PADRAO_REMOCAO = "Sua conta na Bibliotroca foi removida permanentemente devido a uma violação grave dos nossos termos de uso. "
                        +
                        "Lamentamos informar que não será possível reativar o acesso. Entre em contato com nosso suporte para mais informações.";

        private static final String MENSAGEM_PADRAO_REATIVACAO = "Sua conta na Bibliotroca foi reativada. Você já pode fazer login normalmente. "
                        +
                        "Agradecemos sua compreensão e esperamos que tenha uma boa experiência em nossa plataforma.";

        private String motivoEfetivo(String motivo, String padrao) {
                return (motivo != null && !motivo.isBlank()) ? motivo : padrao;
        }

        @Transactional
        public void suspenderCliente(Long id, String motivo, int dias, boolean notificar, String adminEmail) {
                Cliente cliente = clienteRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));

                String motivoFinal = motivoEfetivo(motivo, MOTIVO_PADRAO_SUSPENSAO);
                LocalDateTime agora = LocalDateTime.now();
                cliente.setStatusConta(StatusConta.SUSPENSO);
                cliente.setAtivo(false);
                cliente.setMotivoSuspensao(motivoFinal);
                cliente.setSuspensaoAte(dias > 0 ? agora.plusDays(dias) : null);
                cliente.setDataAcao(agora);
                cliente.setAdminAcao(adminEmail);
                cliente.setEmailNotificadoEm(notificar ? agora : null);
                clienteRepository.save(cliente);

                logAuditoria.registrarLog(
                                AcaoAuditoria.CLIENTE_PERFIL_VISUALIZADO.name(),
                                id,
                                cliente.getEmail(),
                                "Conta suspensa por " + adminEmail + ". Motivo: " + motivoFinal + ". Dias: "
                                                + (dias > 0 ? dias : "indefinido"));

                if (notificar) {
                        String html = EmailHtmlBuilder.contaSuspensa(cliente.getNome(), motivoFinal,
                                        cliente.getSuspensaoAte());
                        emailFacade.sendHtmlSafe(cliente.getEmail(), "Sua conta foi suspensa — Bibliotroca", html);
                }
        }

        @Transactional
        public void removerCliente(Long id, String motivo, boolean notificar, String adminEmail) {
                Cliente cliente = clienteRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));

                String motivoFinal = motivoEfetivo(motivo, MOTIVO_PADRAO_REMOCAO);
                Double saldo = cliente.getSaldoTokens();
                String emailDestino = cliente.getEmail();
                String nomeCliente = cliente.getNome();
                LocalDateTime agora = LocalDateTime.now();

                cliente.setStatusConta(StatusConta.REMOVIDO);
                cliente.setAtivo(false);
                cliente.setMotivoSuspensao(motivoFinal);
                cliente.setDeletedAt(agora);
                cliente.setDataAcao(agora);
                cliente.setAdminAcao(adminEmail);
                cliente.setEmailNotificadoEm(notificar ? agora : null);
                clienteRepository.save(cliente);

                logAuditoria.registrarLog(
                                AcaoAuditoria.CLIENTE_PERFIL_VISUALIZADO.name(),
                                id,
                                emailDestino,
                                "Conta removida por " + adminEmail + ". Motivo: " + motivoFinal);

                if (notificar) {
                        String html = EmailHtmlBuilder.contaRemovida(nomeCliente, motivoFinal, saldo);
                        emailFacade.sendHtmlSafe(emailDestino, "Sua conta foi removida — Bibliotroca", html);
                }
        }

        @Transactional
        public void reativarCliente(Long id, String mensagem, boolean notificar) {
                Cliente cliente = clienteRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));

                String mensagemFinal = motivoEfetivo(mensagem, MENSAGEM_PADRAO_REATIVACAO);

                cliente.setStatusConta(StatusConta.ATIVO);
                cliente.setAtivo(true);
                cliente.setMotivoSuspensao(null);
                cliente.setSuspensaoAte(null);
                cliente.setDeletedAt(null);
                cliente.setDataAcao(null);
                cliente.setAdminAcao(null);
                cliente.setEmailNotificadoEm(notificar ? LocalDateTime.now() : null);
                clienteRepository.save(cliente);

                logAuditoria.registrarLog(
                                AcaoAuditoria.CLIENTE_PERFIL_VISUALIZADO.name(),
                                id,
                                cliente.getEmail(),
                                "Conta reativada pelo admin.");

                if (notificar) {
                        String html = EmailHtmlBuilder.contaReativada(cliente.getNome(), mensagemFinal);
                        emailFacade.sendHtmlSafe(cliente.getEmail(), "Sua conta foi reativada — Bibliotroca", html);
                }
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
                if (totalGasto >= 500)
                        return "Platina";
                if (totalGasto >= 200)
                        return "Ouro";
                if (totalGasto >= 50)
                        return "Prata";
                return "Bronze";
        }

        private String mascararCpf(String cpf) {
                if (cpf == null)
                        return "***.***.***-**";
                String digits = cpf.replaceAll("[^0-9]", "");
                if (digits.length() != 11)
                        return "***.***.***-**";
                return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
        }
}