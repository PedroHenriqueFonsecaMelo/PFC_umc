package umc.exs.service.core.control;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.StatusEnvio;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

/**
 * Gerencia o ciclo de vida dos pedidos (compras de livros).
 * Responsável por criar, listar e atualizar status de envio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

        @Value("${app.base-url:https://localhost:8443}")
        private String baseUrl;

        private final PedidoRepository pedidoRepository;
        private final ClienteRepository clienteRepository;
        private final LogAuditoriaService logAuditoria;
        private final EmailService emailService;
        private final NotificacaoService notificacaoService;

        private static final String CHARSET_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        private static final SecureRandom RNG = new SecureRandom();

        /** Gera um código único no formato BIB-YYYYMMDD-XXXX (não sequencial). */
        private String gerarCodigoPedido() {
                String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                for (int tentativa = 0; tentativa < 100; tentativa++) {
                        StringBuilder sufixo = new StringBuilder(4);
                        for (int i = 0; i < 4; i++) {
                                sufixo.append(CHARSET_CODIGO.charAt(RNG.nextInt(CHARSET_CODIGO.length())));
                        }
                        String codigo = "BIB-" + data + "-" + sufixo;
                        if (!pedidoRepository.existsByCodigoPedido(codigo)) {
                                return codigo;
                        }
                }
                throw new IllegalStateException("Não foi possível gerar um código de pedido único.");
        }

        // ==========================================================
        // CRIAÇÃO
        // ==========================================================

        /**
         * Registra um novo pedido no momento da compra.
         * Preserva todos os dados do livro antes de ele ser deletado da vitrine.
         * LGPD Art. 16 — retenção obrigatória por 5 anos.
         */
        @SuppressWarnings("null")
        @Transactional
        public Pedido registrarPedido(Cliente comprador, Livro livro) {
                LocalDateTime agora = LocalDateTime.now();
                Pedido pedido = Pedido.builder()
                                .comprador(comprador)
                                .livroId(livro.getId())
                                .tituloLivro(livro.getTitulo())
                                .autorLivro(livro.getAutor())
                                .isbnLivro(livro.getIsbn())
                                .fotosUrls(livro.getFotosUrls())
                                .precoLivro(livro.getPrecoAprovado())
                                .statusEnvio(StatusEnvio.AGUARDANDO_ENVIO)
                                .codigoPedido(gerarCodigoPedido())
                                .dataCompra(agora)
                                .dataRetencaoExpira(agora.toLocalDate().plusYears(5))
                                .build();

                Pedido salvo = pedidoRepository.save(pedido);
                log.info("Pedido #{} registrado — livro '{}' para cliente ID {}",
                                salvo.getId(), livro.getTitulo(), comprador.getId());
                return salvo;
        }

        // ==========================================================
        // CONSULTAS
        // ==========================================================

        /** Todos os pedidos do sistema (uso admin), mais recente primeiro. */
        @Transactional(readOnly = true)
        public List<Pedido> listarTodos() {
                return pedidoRepository.findAll(
                                org.springframework.data.domain.Sort.by(
                                                org.springframework.data.domain.Sort.Direction.DESC, "dataCompra"))
                                .stream()
                                .toList();
        }

        /** Todos os pedidos do cliente, mais recente primeiro. */
        @Transactional(readOnly = true)
        public List<Pedido> listarPorCliente(Long compradorId) {
                return pedidoRepository
                                .findByCompradorIdOrderByDataCompraDesc(compradorId)
                                .stream()
                                .toList();
        }

        /** Pedidos com status AGUARDANDO_ENVIO ou EM_TRANSITO (pendentes). */
        @Transactional(readOnly = true)
        public List<Pedido> listarPendentes(Long compradorId) {
                return pedidoRepository
                                .findByCompradorIdAndStatusEnvioNotInOrderByDataCompraDesc(
                                                compradorId,
                                                List.of(StatusEnvio.ENTREGUE, StatusEnvio.CANCELADO))
                                .stream()
                                .toList();
        }

        /** Pedido por ID, validando que pertence ao comprador. */
        @Transactional(readOnly = true)
        public Optional<Pedido> buscarPorIdEComprador(Long pedidoId, Long compradorId) {
                return pedidoRepository.findById(pedidoId)
                                .filter(p -> p.getComprador() != null && compradorId.equals(p.getComprador().getId()));       }

        /** Pedidos com status ENTREGUE (concluídos). */
        @Transactional(readOnly = true)
        public List<Pedido> listarConcluidos(Long compradorId) {
                return pedidoRepository
                                .findByCompradorIdAndStatusEnvioOrderByDataCompraDesc(
                                                compradorId, StatusEnvio.ENTREGUE)
                                .stream()
                                .toList();
        }

        // ==========================================================
        // ATUALIZAÇÃO DE STATUS (admin ou sistema)
        // ==========================================================

        @SuppressWarnings("null")
        @Transactional
        public Pedido atualizarStatus(Long pedidoId, StatusEnvio novoStatus, String codigoRastreio) {
                Pedido pedido = pedidoRepository.findById(pedidoId)
                                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + pedidoId));

                // Impede cancelar um pedido já entregue
                if (pedido.getStatusEnvio() == StatusEnvio.ENTREGUE) {
                        throw new RuntimeException("Não é possível cancelar um pedido já entregue.");
                }

                // Impede atualizar um pedido já cancelado
                if (pedido.getStatusEnvio() == StatusEnvio.CANCELADO) {
                        throw new RuntimeException("Este pedido já está cancelado.");
                }

                // ── ESTORNO: devolve créditos ao comprador ────────────────
                if (novoStatus == StatusEnvio.CANCELADO && pedido.getPrecoLivro() != null) {
                        Cliente comprador = pedido.getComprador();
                        double saldoAnterior = comprador.getSaldoTokens() != null ? comprador.getSaldoTokens() : 0.0;
                        double valorEstorno = pedido.getPrecoLivro();

                        comprador.setSaldoTokens(saldoAnterior + valorEstorno);
                        clienteRepository.save(comprador);

                        log.info("Estorno de T$ {} realizado para o cliente {} (Pedido #{})",
                                        valorEstorno, comprador.getEmail(), pedidoId);

                        logAuditoria.registrarLog(
                                        "PEDIDO_CANCELADO_ESTORNO",
                                        comprador.getId(),
                                        comprador.getEmail(),
                                        String.format("Pedido #%d cancelado — T$ %.2f estornados. Saldo: T$ %.2f → T$ %.2f",
                                                        pedidoId, valorEstorno, saldoAnterior,
                                                        comprador.getSaldoTokens()));

                        // E-mail dedicado de atualização de saldo pelo estorno
                        try {
                                emailService.enviarHtml(
                                        comprador.getEmail(),
                                        "Atualização de saldo — Bibliotroca",
                                        EmailHtmlBuilder.atualizacaoSaldo(
                                                comprador.getNome(),
                                                saldoAnterior,
                                                valorEstorno,
                                                comprador.getSaldoTokens(),
                                                "Estorno — pedido #" + pedidoId + " cancelado",
                                                true, LocalDateTime.now()));
                        } catch (Exception e) {
                                log.error("Falha ao enviar e-mail de estorno para {}: {}", comprador.getEmail(), e.getMessage());
                        }

                        // Notificação dashboard: estorno
                        try {
                                notificacaoService.criarNotificacaoDashboard(
                                        comprador,
                                        String.format("Seu pedido #%d foi cancelado. T$ %.2f foram devolvidos ao seu saldo.",
                                                pedidoId, valorEstorno),
                                        "/clientes/homepage?aba=pedidos");
                        } catch (Exception e) {
                                log.error("Falha ao criar notificação de estorno: {}", e.getMessage());
                        }
                }

                pedido.setStatusEnvio(novoStatus);
                pedido.setDataAtualizacaoStatus(LocalDateTime.now());
                if (codigoRastreio != null && !codigoRastreio.isBlank()) {
                        pedido.setCodigoRastreio(codigoRastreio);
                }

                Pedido salvo = pedidoRepository.save(pedido);

                logAuditoria.registrarLog(
                                "PEDIDO_STATUS_ATUALIZADO",
                                pedido.getComprador().getId(),
                                pedido.getComprador().getEmail(),
                                "Pedido #" + pedidoId + " → " + novoStatus.getDescricao());

                // E-mail ao comprador informando mudança de status
                if (pedido.getComprador() != null) {
                        try {
                                Cliente compradorPedido = pedido.getComprador();
                                boolean cancelado = novoStatus == StatusEnvio.CANCELADO
                                        && pedido.getPrecoLivro() != null;
                                emailService.enviarHtml(
                                        compradorPedido.getEmail(),
                                        "Atualização do pedido #" + pedidoId + " — Bibliotroca",
                                        EmailHtmlBuilder.atualizacaoPedido(
                                                compradorPedido.getNome(), pedidoId,
                                                novoStatus.getDescricao(), pedido.getTituloLivro(),
                                                codigoRastreio, cancelado,
                                                cancelado ? pedido.getPrecoLivro() : 0.0,
                                                baseUrl));
                        } catch (Exception e) {
                                log.error("Falha ao enviar e-mail de status do pedido #{}: {}", pedidoId, e.getMessage());
                        }

                        // Notificação dashboard para status não-cancelados (cancelado já notificou acima)
                        if (novoStatus != StatusEnvio.CANCELADO) {
                                try {
                                        notificacaoService.criarNotificacaoDashboard(
                                                pedido.getComprador(),
                                                String.format("Seu pedido #%d foi atualizado para: %s.",
                                                        pedidoId, novoStatus.getDescricao()),
                                                "/clientes/homepage?aba=pedidos");
                                } catch (Exception e) {
                                        log.error("Falha ao criar notificação de status do pedido #{}: {}", pedidoId, e.getMessage());
                                }
                        }
                }

                return salvo;
        }
}

