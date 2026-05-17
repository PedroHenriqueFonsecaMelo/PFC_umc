package umc.exs.service.cancelamento;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.cancelamento.SolicitacaoCancelamentoRequestDTO;
import umc.exs.dto.cancelamento.SolicitacaoCancelamentoResponseDTO;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.MotivoCategoria;
import umc.exs.model.enums.StatusEnvio;
import umc.exs.model.enums.StatusSolicitacao;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.SolicitacaoCancelamentoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
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
    private final EmailService emailService;
    private final LogAuditoriaService logAuditoria;

    // ──────────────────────────────────────────────────────────────────────
    // CLIENTE: solicitar cancelamento
    // ──────────────────────────────────────────────────────────────────────

    @Transactional
    public SolicitacaoCancelamentoResponseDTO solicitarCancelamento(
            Long pedidoId, String emailCliente, SolicitacaoCancelamentoRequestDTO request) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + pedidoId));

        // Valida que o pedido pertence ao cliente autenticado
        if (!pedido.getComprador().getEmail().equals(emailCliente)) {
            throw new IllegalStateException("Acesso negado ao pedido #" + pedidoId);
        }

        // Só pode solicitar para pedidos AGUARDANDO_ENVIO
        if (pedido.getStatusEnvio() != StatusEnvio.AGUARDANDO_ENVIO) {
            throw new IllegalStateException(
                "Cancelamento só é possível enquanto o pedido está aguardando envio.");
        }

        // Impede duplicata
        if (cancelamentoRepository.existsByPedidoIdAndStatus(pedidoId, StatusSolicitacao.PENDENTE)) {
            throw new IllegalStateException("Já existe uma solicitação de cancelamento pendente para este pedido.");
        }

        // Valida campos obrigatórios
        if (request.getMotivoCategoria() == null) {
            throw new IllegalArgumentException("Motivo da categoria é obrigatório.");
        }
        if (request.getMotivoDescricao() == null || request.getMotivoDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição do motivo é obrigatória.");
        }

        Cliente cliente = pedido.getComprador();

        SolicitacaoCancelamento solicitacao = SolicitacaoCancelamento.builder()
                .pedido(pedido)
                .cliente(cliente)
                .motivoCategoria(request.getMotivoCategoria())
                .motivoDescricao(request.getMotivoDescricao().trim())
                .status(StatusSolicitacao.PENDENTE)
                .build();

        cancelamentoRepository.save(solicitacao);

        logAuditoria.registrarLog("CANCELAMENTO_SOLICITADO", cliente.getId(), cliente.getEmail(),
                "Pedido #" + pedidoId + " — " + request.getMotivoCategoria().getDescricao());

        // Notificação dashboard ao cliente
        try {
            notificacaoService.criarNotificacaoDashboard(
                    cliente,
                    String.format("Sua solicitação de cancelamento do pedido #%d foi recebida e está em análise.", pedidoId),
                    "/clientes/homepage?aba=pedidos");
        } catch (Exception e) {
            log.error("Erro ao notificar cliente sobre solicitação de cancelamento: {}", e.getMessage());
        }

        return toDTO(solicitacao);
    }

    // ──────────────────────────────────────────────────────────────────────
    // ADMIN: listar todas / pendentes
    // ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SolicitacaoCancelamentoResponseDTO> listarTodas() {
        return cancelamentoRepository.findAllByOrderByDataSolicitacaoDesc()
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoCancelamentoResponseDTO> listarPendentes() {
        return cancelamentoRepository.findByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao.PENDENTE)
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public long contarPendentes() {
        return cancelamentoRepository.countByStatus(StatusSolicitacao.PENDENTE);
    }

    // ──────────────────────────────────────────────────────────────────────
    // ADMIN: aprovar cancelamento (com estorno)
    // ──────────────────────────────────────────────────────────────────────

    @Transactional
    public SolicitacaoCancelamentoResponseDTO aprovarCancelamento(Long solicitacaoId, String comentarioAdmin) {
        log.info("Admin aprovando cancelamento solicitacaoId={}", solicitacaoId);
        SolicitacaoCancelamento sol = buscarPendente(solicitacaoId);

        sol.setStatus(StatusSolicitacao.APROVADO);
        sol.setComentarioAdmin(comentarioAdmin);
        sol.setDataResposta(LocalDateTime.now());
        cancelamentoRepository.save(sol);

        // Atualiza pedido para CANCELADO
        Pedido pedido = sol.getPedido();
        pedido.setStatusEnvio(StatusEnvio.CANCELADO);
        pedido.setDataAtualizacaoStatus(LocalDateTime.now());
        pedidoRepository.save(pedido);

        // Devolve o livro para a vitrine
        try {
            livroRepository.findById(pedido.getLivroId()).ifPresent(livro -> {
                livro.setAprovado(true);
                livroRepository.save(livro);
                log.info("Livro id={} devolvido à vitrine após cancelamento aprovado.", livro.getId());
            });
        } catch (Exception e) {
            log.error("Erro ao devolver livro à vitrine após cancelamento: {}", e.getMessage());
        }

        // Realiza estorno
        Cliente cliente = sol.getCliente();
        double valorEstorno = pedido.getPrecoLivro() != null ? pedido.getPrecoLivro() : 0.0;
        double saldoAnterior = cliente.getSaldoTokens() != null ? cliente.getSaldoTokens() : 0.0;
        cliente.setSaldoTokens(saldoAnterior + valorEstorno);
        clienteRepository.save(cliente);

        logAuditoria.registrarLog("CANCELAMENTO_APROVADO", cliente.getId(), cliente.getEmail(),
                String.format("Pedido #%d cancelado — T$ %.2f estornados.", pedido.getId(), valorEstorno));

        // Notificação dashboard
        try {
            notificacaoService.criarNotificacaoDashboard(
                    cliente,
                    String.format("Seu cancelamento do pedido #%d foi aprovado. T$ %.2f foram devolvidos ao seu saldo.",
                            pedido.getId(), valorEstorno),
                    "/clientes/homepage?aba=pedidos");
        } catch (Exception e) {
            log.error("Erro ao notificar cliente sobre cancelamento aprovado: {}", e.getMessage());
        }

        // E-mail ao cliente
        try {
            emailService.enviarHtml(
                    cliente.getEmail(),
                    "Cancelamento aprovado — Bibliotroca",
                    EmailHtmlBuilder.cancelamentoAprovado(
                            cliente.getNome(), pedido.getId(), pedido.getTituloLivro(),
                            valorEstorno, cliente.getSaldoTokens(), comentarioAdmin));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de cancelamento aprovado: {}", e.getMessage());
        }

        return toDTO(sol);
    }

    // ──────────────────────────────────────────────────────────────────────
    // ADMIN: recusar cancelamento
    // ──────────────────────────────────────────────────────────────────────

    @Transactional
    public SolicitacaoCancelamentoResponseDTO recusarCancelamento(Long solicitacaoId, String comentarioAdmin) {
        log.info("Admin recusando cancelamento solicitacaoId={}", solicitacaoId);
        SolicitacaoCancelamento sol = buscarPendente(solicitacaoId);

        sol.setStatus(StatusSolicitacao.RECUSADO);
        sol.setComentarioAdmin(comentarioAdmin);
        sol.setDataResposta(LocalDateTime.now());
        cancelamentoRepository.save(sol);

        Pedido pedido = sol.getPedido();
        Cliente cliente = sol.getCliente();

        logAuditoria.registrarLog("CANCELAMENTO_RECUSADO", cliente.getId(), cliente.getEmail(),
                "Pedido #" + pedido.getId() + " — cancelamento recusado.");

        // Notificação dashboard
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

        // E-mail ao cliente
        try {
            emailService.enviarHtml(
                    cliente.getEmail(),
                    "Cancelamento não aprovado — Bibliotroca",
                    EmailHtmlBuilder.cancelamentoRecusado(
                            cliente.getNome(), pedido.getId(), pedido.getTituloLivro(), comentarioAdmin));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de cancelamento recusado: {}", e.getMessage());
        }

        return toDTO(sol);
    }

    // ──────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────

    private SolicitacaoCancelamento buscarPendente(Long solicitacaoId) {
        SolicitacaoCancelamento sol = cancelamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> {
                    log.error("Solicitação de cancelamento não encontrada: {}", solicitacaoId);
                    return new IllegalArgumentException("Solicitação não encontrada: " + solicitacaoId);
                });
        if (sol.getStatus() != StatusSolicitacao.PENDENTE) {
            log.warn("Solicitação {} já foi processada (status={})", solicitacaoId, sol.getStatus());
            throw new IllegalStateException("Esta solicitação já foi processada.");
        }
        return sol;
    }

    @Transactional(readOnly = true)
    public SolicitacaoCancelamentoResponseDTO buscarPorId(Long solicitacaoId) {
        SolicitacaoCancelamento sol = cancelamentoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada: " + solicitacaoId));
        return toDTO(sol);
    }

    private SolicitacaoCancelamentoResponseDTO toDTO(SolicitacaoCancelamento s) {
        SolicitacaoCancelamentoResponseDTO dto = new SolicitacaoCancelamentoResponseDTO();
        dto.setId(s.getId());
        dto.setPedidoId(s.getPedido().getId());
        dto.setTituloLivro(s.getPedido().getTituloLivro());
        dto.setAutorLivro(s.getPedido().getAutorLivro());
        dto.setFotosUrls(s.getPedido().getFotosUrls());
        dto.setPrecoLivro(s.getPedido().getPrecoLivro());
        dto.setIsbnLivro(s.getPedido().getIsbnLivro());
        dto.setDataCompra(s.getPedido().getDataCompra());
        dto.setSaldoAtualComprador(s.getCliente().getSaldoTokens());
        dto.setClienteNome(s.getCliente().getNome());
        dto.setClienteEmail(s.getCliente().getEmail());
        dto.setMotivoCategoria(s.getMotivoCategoria());
        dto.setMotivoCategoriaDescricao(s.getMotivoCategoria() != null
                ? s.getMotivoCategoria().getDescricao() : null);
        dto.setMotivoDescricao(s.getMotivoDescricao());
        dto.setStatus(s.getStatus());
        dto.setStatusDescricao(s.getStatus() != null ? s.getStatus().getDescricao() : null);
        dto.setComentarioAdmin(s.getComentarioAdmin());
        dto.setDataSolicitacao(s.getDataSolicitacao());
        dto.setDataResposta(s.getDataResposta());
        return dto;
    }
}
