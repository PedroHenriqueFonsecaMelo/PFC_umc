package umc.exs.service.cancelamento;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.request.admin.CancelamentoRequest;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.StatusEnvio;
import umc.exs.model.enums.StatusSolicitacao;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.SolicitacaoCancelamentoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelamentoService {

    private final SolicitacaoCancelamentoRepository cancelamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final NotificacaoService notificacaoService;
    private final EmailFacade emailFacade;
    private final LogAuditoriaService logAuditoria;

    @Transactional
    public SolicitacaoCancelamento solicitarCancelamento(
            Long pedidoId, String emailCliente, CancelamentoRequest request) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + pedidoId));

        if (!pedido.getComprador().getEmail().equals(emailCliente)) {
            throw new IllegalStateException("Acesso negado ao pedido #" + pedidoId);
        }

        if (pedido.getStatusEnvio() != StatusEnvio.AGUARDANDO_ENVIO) {
            throw new IllegalStateException("Cancelamento só é possível enquanto o pedido está aguardando envio.");
        }

        if (cancelamentoRepository.existsByPedidoIdAndStatus(pedidoId, StatusSolicitacao.PENDENTE)) {
            throw new IllegalStateException("Já existe uma solicitação de cancelamento pendente para este pedido.");
        }

        if (request.getMotivoCategoria() == null) {
            throw new IllegalArgumentException("Motivo da categoria é obrigatório.");
        }

        if (request.getMotivoDescricao() == null || request.getMotivoDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição do motivo é obrigatória.");
        }

        Cliente cliente = pedido.getComprador();

        SolicitacaoCancelamento sol = SolicitacaoCancelamento.builder()
                .pedido(pedido)
                .cliente(cliente)
                .motivoCategoria(request.getMotivoCategoria())
                .motivoDescricao(request.getMotivoDescricao().trim())
                .status(StatusSolicitacao.PENDENTE)
                .build();

        cancelamentoRepository.save(sol);

        logAuditoria.registrarLog(
                AcaoAuditoria.CANCELAMENTO_SOLICITADO.name(),
                cliente.getId(),
                cliente.getEmail(),
                "Pedido #" + pedidoId + " — " + request.getMotivoCategoria().getDescricao());

        try {
            notificacaoService.criarNotificacaoDashboard(
                    cliente,
                    String.format("Sua solicitação de cancelamento do pedido #%d foi recebida e está em análise.",
                            pedidoId),
                    "/clientes/homepage?aba=pedidos");
        } catch (Exception e) {
            log.error("Erro ao notificar cliente sobre solicitação de cancelamento: {}", e.getMessage());
        }

        return sol;
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoCancelamento> listarTodas() {
        return cancelamentoRepository.findAllByOrderByDataSolicitacaoDesc();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoCancelamento> listarPendentes() {
        return cancelamentoRepository.findByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao.PENDENTE);
    }

    @Transactional(readOnly = true)
    public long contarPendentes() {
        return cancelamentoRepository.countByStatus(StatusSolicitacao.PENDENTE);
    }

    @Transactional
    public SolicitacaoCancelamento aprovarCancelamento(Long solicitacaoId, String comentarioAdmin) {

        SolicitacaoCancelamento sol = buscarPendente(solicitacaoId);

        sol.setStatus(StatusSolicitacao.APROVADO);
        sol.setComentarioAdmin(comentarioAdmin);
        sol.setDataResposta(LocalDateTime.now());
        cancelamentoRepository.save(sol);

        Pedido pedido = sol.getPedido();
        Cliente cliente = sol.getCliente();

        pedido.setStatusEnvio(StatusEnvio.CANCELADO);
        pedido.setDataAtualizacaoStatus(LocalDateTime.now());
        pedidoRepository.save(pedido);

        livroRepository.findById(pedido.getLivroId()).ifPresent(livro -> {
            livro.setAprovado(true);
            livroRepository.save(livro);
        });

        double valorEstorno = pedido.getPrecoLivro() != null ? pedido.getPrecoLivro() : 0.0;
        double saldoAnterior = cliente.getSaldoTokens() != null ? cliente.getSaldoTokens() : 0.0;

        cliente.setSaldoTokens(saldoAnterior + valorEstorno);
        clienteRepository.save(cliente);

        logAuditoria.registrarLog(
                AcaoAuditoria.CANCELAMENTO_APROVADO.name(),
                cliente.getId(),
                cliente.getEmail(),
                String.format("Pedido #%d cancelado — T$ %.2f estornados.", pedido.getId(), valorEstorno));

        try {
            notificacaoService.criarNotificacaoDashboard(
                    cliente,
                    String.format("Seu cancelamento do pedido #%d foi aprovado. T$ %.2f foram devolvidos ao seu saldo.",
                            pedido.getId(), valorEstorno),
                    "/clientes/homepage?aba=pedidos");
        } catch (Exception e) {
            log.error("Erro ao notificar cliente sobre cancelamento aprovado: {}", e.getMessage());
        }

        try {
            emailFacade.sendHtmlSafe(
                    cliente.getEmail(),
                    "Cancelamento aprovado — Bibliotroca",
                    EmailHtmlBuilder.cancelamentoAprovado(
                            cliente.getNome(),
                            pedido.getId(),
                            pedido.getTituloLivro(),
                            valorEstorno,
                            cliente.getSaldoTokens(),
                            comentarioAdmin));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de cancelamento aprovado: {}", e.getMessage());
        }

        return sol;
    }

    @Transactional
    public SolicitacaoCancelamento recusarCancelamento(Long solicitacaoId, String comentarioAdmin) {

        SolicitacaoCancelamento sol = buscarPendente(solicitacaoId);

        sol.setStatus(StatusSolicitacao.RECUSADO);
        sol.setComentarioAdmin(comentarioAdmin);
        sol.setDataResposta(LocalDateTime.now());
        cancelamentoRepository.save(sol);

        Pedido pedido = sol.getPedido();
        Cliente cliente = sol.getCliente();

        logAuditoria.registrarLog(
                AcaoAuditoria.CANCELAMENTO_RECUSADO.name(),
                cliente.getId(),
                cliente.getEmail(),
                "Pedido #" + pedido.getId() + " — cancelamento recusado.");

        try {
            String preview = (comentarioAdmin != null && !comentarioAdmin.isBlank())
                    ? " Motivo: " + comentarioAdmin
                    : "";

            notificacaoService.criarNotificacaoDashboard(
                    cliente,
                    String.format("Sua solicitação de cancelamento do pedido #%d foi recusada.%s",
                            pedido.getId(), preview),
                    "/clientes/homepage?aba=pedidos");
        } catch (Exception e) {
            log.error("Erro ao notificar cliente sobre cancelamento recusado: {}", e.getMessage());
        }

        try {
            emailFacade.sendHtmlSafe(
                    cliente.getEmail(),
                    "Cancelamento não aprovado — Bibliotroca",
                    EmailHtmlBuilder.cancelamentoRecusado(
                            cliente.getNome(),
                            pedido.getId(),
                            pedido.getTituloLivro(),
                            comentarioAdmin));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de cancelamento recusado: {}", e.getMessage());
        }

        return sol;
    }

    private SolicitacaoCancelamento buscarPendente(Long solicitacaoId) {
        SolicitacaoCancelamento sol = cancelamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));

        if (sol.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new IllegalStateException("Esta solicitação já foi processada.");
        }

        return sol;
    }

    @Transactional
    public Map<String, Object> cancelarPeloAdmin(
            Long pedidoId,
            umc.exs.model.enums.MotivoCategoria motivo,
            String justificativa) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

        Cliente comprador = pedido.getComprador();

        double preco = pedido.getPrecoLivro() != null ? pedido.getPrecoLivro() : 0.0;
        double saldoAnterior = comprador.getSaldoTokens() != null ? comprador.getSaldoTokens() : 0.0;

        comprador.setSaldoTokens(saldoAnterior + preco);
        clienteRepository.save(comprador);

        livroRepository.findById(pedido.getLivroId()).ifPresent(livro -> {
            livro.setAprovado(true);
            livroRepository.save(livro);
        });

        pedido.setStatusEnvio(StatusEnvio.CANCELADO);
        pedido.setDataAtualizacaoStatus(LocalDateTime.now());
        pedidoRepository.save(pedido);

        SolicitacaoCancelamento registro = SolicitacaoCancelamento.builder()
                .pedido(pedido)
                .cliente(comprador)
                .motivoCategoria(motivo)
                .motivoDescricao(justificativa)
                .status(StatusSolicitacao.APROVADO)
                .comentarioAdmin("Cancelamento realizado pelo administrador.")
                .dataResposta(LocalDateTime.now())
                .build();

        cancelamentoRepository.save(registro);

        logAuditoria.registrarLog(
                AcaoAuditoria.CANCELAMENTO_ADMIN.name(),
                comprador.getId(),
                comprador.getEmail(),
                String.format("Pedido #%d cancelado pelo admin — T$ %.2f estornados. Motivo: %s",
                        pedidoId, preco, motivo.getDescricao()));

        return Map.of(
                "cancelado", true,
                "pedidoId", pedidoId,
                "precoLivro", preco,
                "saldoAposEstorno", comprador.getSaldoTokens());
    }

    @Transactional(readOnly = true)
    public SolicitacaoCancelamento buscarPorId(Long solicitacaoId) {
        return cancelamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada: " + solicitacaoId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarCancelamentosCliente(String email) {

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        return cancelamentoRepository.findByClienteIdOrderByDataSolicitacaoDesc(cliente.getId())
                .stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("pedidoId", c.getPedido().getId());
                    m.put("tituloLivro", c.getPedido().getTituloLivro());
                    m.put("precoLivro", c.getPedido().getPrecoLivro());
                    m.put("motivoCategoria", c.getMotivoCategoria() != null ? c.getMotivoCategoria().getDescricao() : "—");
                    m.put("motivoDescricao", c.getMotivoDescricao());
                    m.put("status", c.getStatus() != null ? c.getStatus().getDescricao() : "—");
                    m.put("comentarioAdmin", c.getComentarioAdmin());
                    m.put("dataSolicitacao", c.getDataSolicitacao());
                    m.put("dataResposta", c.getDataResposta());
                    m.put("canceladoPeloAdmin", c.getComentarioAdmin() != null && c.getComentarioAdmin().contains("administrador"));
                    return m;
                })
                .toList();
    }
}