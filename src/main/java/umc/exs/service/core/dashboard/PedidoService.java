package umc.exs.service.core.dashboard;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
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
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

/**
 * Service de pedidos: cria, lista, atualiza status e envia notificações.
 * Foco: orquestração. Lógica complexa isolada em métodos privados.
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
        private final EmailFacade emailFacade;
        private final NotificacaoService notificacaoService;

        private static final String CHARSET_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        private static final SecureRandom RNG = new SecureRandom();

        // =========================
        // GERAÇÃO DE CÓDIGO
        // =========================
        public String gerarCodigoPedido() {
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

        // =========================
        // CRIAÇÃO DE PEDIDO
        // =========================
        @Transactional
        public Pedido registrarPedido(Cliente comprador, Livro livro, String codigoPedido) {
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
                                .codigoPedido(codigoPedido)
                                .dataCompra(agora)
                                .dataRetencaoExpira(agora.toLocalDate().plusYears(5))
                                .build();

                Pedido salvo = pedidoRepository.save(pedido);
                log.info("Pedido #{} registrado — livro '{}' para cliente ID {} (compra #{})",
                                salvo.getId(), livro.getTitulo(), comprador.getId(), codigoPedido);
                return salvo;
        }

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

        // =========================
        // LISTAGENS
        // =========================
        @Transactional(readOnly = true)
        public List<Pedido> listarTodos() {
                return pedidoRepository.findAllByOrderByDataCompraDesc();
        }

        @Transactional(readOnly = true)
        public List<Pedido> listarPorCliente(Long compradorId) {
                return pedidoRepository.findByCompradorIdOrderByDataCompraDesc(compradorId);
        }

        @Transactional(readOnly = true)
        public List<Pedido> listarPendentes(Long compradorId) {
                return pedidoRepository.findByCompradorIdAndStatusEnvioNotInOrderByDataCompraDesc(
                                compradorId, List.of(StatusEnvio.ENTREGUE, StatusEnvio.CANCELADO));
        }

        @Transactional(readOnly = true)
        public Optional<Pedido> buscarPorIdEComprador(Long pedidoId, Long compradorId) {
                return pedidoRepository.findById(pedidoId)
                                .filter(p -> p.getComprador() != null && compradorId.equals(p.getComprador().getId()));
        }

        @Transactional(readOnly = true)
        public List<Pedido> listarConcluidos(Long compradorId) {
                return pedidoRepository.findByCompradorIdAndStatusEnvioOrderByDataCompraDesc(
                                compradorId, StatusEnvio.ENTREGUE);
        }

        // =========================
        // ATUALIZAÇÃO DE STATUS
        // =========================
        @Transactional
        public Pedido atualizarStatus(Long pedidoId, StatusEnvio novoStatus, String codigoRastreio) {
                Pedido pedido = pedidoRepository.findById(pedidoId)
                                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + pedidoId));

                validarStatusAtual(pedido, novoStatus);

                if (novoStatus == StatusEnvio.CANCELADO) {
                        processarCancelamento(pedido);
                }

                pedido.setStatusEnvio(novoStatus);
                pedido.setDataAtualizacaoStatus(LocalDateTime.now());
                if (codigoRastreio != null && !codigoRastreio.isBlank()) {
                        pedido.setCodigoRastreio(codigoRastreio);
                }

                Pedido salvo = pedidoRepository.save(pedido);

                registrarLog("PEDIDO_STATUS_ATUALIZADO", pedido.getComprador(),
                                "Pedido #" + pedidoId + " → " + novoStatus.getDescricao());

                enviarNotificacoesStatus(pedido, novoStatus);

                return salvo;
        }

        // =========================
        // MÉTODOS PRIVADOS
        // =========================
        private void validarStatusAtual(Pedido pedido, StatusEnvio novoStatus) {
                if (pedido.getStatusEnvio() == StatusEnvio.ENTREGUE) {
                        throw new RuntimeException("Não é possível cancelar um pedido já entregue.");
                }
                if (pedido.getStatusEnvio() == StatusEnvio.CANCELADO) {
                        throw new RuntimeException("Este pedido já está cancelado.");
                }
        }

        private void processarCancelamento(Pedido pedido) {
                Cliente comprador = pedido.getComprador();
                double saldoAnterior = comprador.getSaldoTokens() != null ? comprador.getSaldoTokens() : 0.0;
                double valorEstorno = pedido.getPrecoLivro();

                comprador.setSaldoTokens(saldoAnterior + valorEstorno);
                clienteRepository.save(comprador);

                registrarLog("PEDIDO_CANCELADO_ESTORNO", comprador,
                                String.format("Pedido #%d cancelado — T$ %.2f estornados. Saldo: T$ %.2f → T$ %.2f",
                                                pedido.getId(), valorEstorno, saldoAnterior,
                                                comprador.getSaldoTokens()));

                enviarEmailEstorno(comprador, pedido.getId(), saldoAnterior, valorEstorno);
                criarNotificacaoEstorno(comprador, pedido.getId(), valorEstorno);
        }

        private void enviarEmailEstorno(Cliente comprador, Long pedidoId, double saldoAnterior, double valorEstorno) {
                try {
                        emailFacade.sendHtmlSafe(
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
                        log.error("Falha ao enviar e-mail de estorno para {}: {}", comprador.getEmail(),
                                        e.getMessage());
                }
        }

        private void criarNotificacaoEstorno(Cliente comprador, Long pedidoId, double valorEstorno) {
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

        private void enviarNotificacoesStatus(Pedido pedido, StatusEnvio novoStatus) {
                if (pedido.getComprador() == null)
                        return;

                boolean cancelado = novoStatus == StatusEnvio.CANCELADO && pedido.getPrecoLivro() != null;
                try {
                        emailFacade.sendHtmlSafe(
                                        pedido.getComprador().getEmail(),
                                        "Atualização do pedido #" + pedido.getId() + " — Bibliotroca",
                                        EmailHtmlBuilder.atualizacaoPedido(
                                                        pedido.getComprador().getNome(),
                                                        pedido.getId(),
                                                        novoStatus.getDescricao(),
                                                        pedido.getTituloLivro(),
                                                        pedido.getCodigoRastreio(),
                                                        cancelado,
                                                        cancelado ? pedido.getPrecoLivro() : 0.0,
                                                        baseUrl));
                } catch (Exception e) {
                        log.error("Falha ao enviar e-mail de status do pedido #{}: {}", pedido.getId(), e.getMessage());
                }

                if (!cancelado) {
                        try {
                                notificacaoService.criarNotificacaoDashboard(
                                                pedido.getComprador(),
                                                String.format("Seu pedido #%d foi atualizado para: %s.", pedido.getId(),
                                                                novoStatus.getDescricao()),
                                                "/clientes/homepage?aba=pedidos");
                        } catch (Exception e) {
                                log.error("Falha ao criar notificação de status do pedido #{}: {}", pedido.getId(),
                                                e.getMessage());
                        }
                }
        }

        private void registrarLog(String tipo, Cliente cliente, String descricao) {
                if (cliente != null) {
                        logAuditoria.registrarLog(tipo, cliente.getId(), cliente.getEmail(), descricao);
                }
        }
}