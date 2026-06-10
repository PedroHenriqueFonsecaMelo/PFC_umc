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
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;
import umc.exs.service.notificacao.NotificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

        @Value("${app.base-url:https://localhost:8443}")
        private String baseUrl;

        private final PedidoRepository pedidoRepository;
        private final ClienteRepository clienteRepository;
        private final AppLogger appLogger;
        private final EmailFacade emailFacade;
        private final NotificacaoService notificacaoService;

        private static final String CHARSET_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        private static final SecureRandom RNG = new SecureRandom();

        public String gerarCodigoPedido() {
                String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                for (int i = 0; i < 100; i++) {
                        StringBuilder sufixo = new StringBuilder(4);
                        for (int j = 0; j < 4; j++) {
                                sufixo.append(CHARSET_CODIGO.charAt(RNG.nextInt(CHARSET_CODIGO.length())));
                        }
                        String codigo = "BIB-" + data + "-" + sufixo;
                        if (!pedidoRepository.existsByCodigoPedido(codigo)) {
                                return codigo;
                        }
                }
                throw new IllegalStateException("Falha ao gerar código de pedido");
        }

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
                                .precoLivro(livro.getPrecoAprovado()) // Certifique-se de aplicar o desconto aqui se houver cupom!
                                .statusEnvio(StatusEnvio.AGUARDANDO_ENVIO)
                                .codigoPedido(codigoPedido)
                                .dataCompra(agora)
                                .dataRetencaoExpira(agora.toLocalDate().plusYears(5))
                                .build();

                Pedido salvo = pedidoRepository.save(pedido);
                appLogger.success(AcaoAuditoria.PAGAMENTO_INTENCAO_REGISTRADA, comprador.getId(), comprador.getEmail(), "Pedido criado ID=" + salvo.getId());
                return salvo;
        }

        @Transactional(readOnly = true)
        public List<Pedido> listarTodos() { return pedidoRepository.findAllByOrderByDataCompraDesc(); }

        @Transactional(readOnly = true)
        public List<Pedido> listarPorCliente(Long compradorId) { return pedidoRepository.findByCompradorIdOrderByDataCompraDesc(compradorId); }

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
                return pedidoRepository.findByCompradorIdAndStatusEnvioOrderByDataCompraDesc(compradorId, StatusEnvio.ENTREGUE);
        }

        @Transactional
        public Pedido atualizarStatus(Long pedidoId, StatusEnvio novoStatus, String codigoRastreio) {
                Pedido pedido = pedidoRepository.findComCompradorEEnderecos(pedidoId)
                                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

                validarStatusAtual(pedido, novoStatus);

                // 1. Atualiza as propriedades no objeto primeiro para que as notificações leiam o dado correto
                pedido.setStatusEnvio(novoStatus);
                pedido.setDataAtualizacaoStatus(LocalDateTime.now());
                if (codigoRastreio != null && !codigoRastreio.isBlank()) {
                        pedido.setCodigoRastreio(codigoRastreio);
                }

                // 2. Trata fluxos específicos de e-mail/estorno com base no status novo
                if (novoStatus == StatusEnvio.CANCELADO) {
                        processarCancelamento(pedido); 
                } else {
                        // Só envia e-mail de atualização comum se NÃO for cancelamento (evita spam de e-mails duplicados)
                        enviarNotificacoesStatus(pedido, novoStatus);
                }

                Pedido salvo = pedidoRepository.save(pedido);

                appLogger.success(
                                AcaoAuditoria.PAGAMENTO_PIX_CONFIRMADO,
                                pedido.getComprador().getId(),
                                pedido.getComprador().getEmail(),
                                "Status atualizado pedido=" + pedidoId + " status=" + novoStatus);

                log.info("PEDIDO_STATUS_ATUALIZADO id={} status={}", pedidoId, novoStatus);
                return salvo;
        }

        private void validarStatusAtual(Pedido pedido, StatusEnvio novoStatus) {
                if (pedido.getStatusEnvio() == StatusEnvio.ENTREGUE) {
                        throw new RuntimeException("Pedido já entregue");
                }
                if (pedido.getStatusEnvio() == StatusEnvio.CANCELADO) {
                        throw new RuntimeException("Pedido já cancelado");
                }
        }

        private void processarCancelamento(Pedido pedido) {
                Cliente cliente = pedido.getComprador();
                double saldoAnterior = cliente.getSaldoTokens() != null ? cliente.getSaldoTokens() : 0.0;
                
                // Correção do Valor: Garante o valor que de fato saiu da carteira do cliente
                double valorEstorno = pedido.getPrecoLivro(); 

                // Executa as notificações de estorno ANTES de mutar o saldo para não corromper o e-mail
                enviarEmailEstorno(cliente, pedido.getId(), saldoAnterior, valorEstorno);
                criarNotificacaoEstorno(cliente, pedido.getId(), valorEstorno);

                // Modifica e salva o saldo atualizado no banco
                cliente.setSaldoTokens(saldoAnterior + valorEstorno);
                clienteRepository.save(cliente);

                appLogger.success(
                                AcaoAuditoria.CANCELAMENTO_APROVADO,
                                cliente.getId(),
                                cliente.getEmail(),
                                "Estorno pedido=" + pedido.getId());

                log.info("PEDIDO_CANCELADO id={} clienteId={} estorno={}", pedido.getId(), cliente.getId(), valorEstorno);
        }

        private void enviarEmailEstorno(Cliente cliente, Long pedidoId, double saldoAnterior, double valor) {
                try {
                        // Correção: Passando o saldoAnterior real capturado antes da mutação do banco
                        double saldoAtualizado = saldoAnterior + valor; 

                        emailFacade.sendHtmlSafe(
                                        cliente.getEmail(),
                                        "Estorno confirmado — Pedido #" + pedidoId,
                                        EmailHtmlBuilder.atualizacaoSaldo(
                                                        cliente.getNome(),
                                                        saldoAnterior,
                                                        valor,
                                                        saldoAtualizado, 
                                                        "Estorno do pedido #" + pedidoId + " (Tokens devolvidos à sua carteira)",
                                                        true,
                                                        LocalDateTime.now()));
                } catch (Exception e) {
                        log.error("ERRO_EMAIL_ESTORNO pedidoId={} email={}", pedidoId, cliente.getEmail(), e);
                }
        }

        private void criarNotificacaoEstorno(Cliente cliente, Long pedidoId, double valor) {
                try {
                        notificacaoService.criarNotificacaoDashboard(
                                        cliente,
                                        "Estorno realizado pedido #" + pedidoId + " valor T$ " + valor,
                                        "/clientes/homepage?aba=pedidos");
                } catch (Exception e) {
                        log.error("ERRO_NOTIFICACAO_ESTORNO pedidoId={}", pedidoId, e);
                }
        }

        private void enviarNotificacoesStatus(Pedido pedido, StatusEnvio status) {
                if (pedido.getComprador() == null) return;

                try {
                        // Validação do código de rastreio para exibição limpa no e-mail
                        String rastreio = (pedido.getCodigoRastreio() != null && !pedido.getCodigoRastreio().isBlank()) 
                                        ? pedido.getCodigoRastreio() 
                                        : "Não aplicável / Em processamento";

                        emailFacade.sendHtmlSafe(
                                        pedido.getComprador().getEmail(),
                                        "Atualização do seu Pedido #" + pedido.getId(),
                                        EmailHtmlBuilder.atualizacaoPedido(
                                                        pedido.getComprador().getNome(),
                                                        pedido.getId(),
                                                        status.getDescricao(),
                                                        pedido.getTituloLivro(),
                                                        rastreio, // Código de rastreio higienizado enviado aqui
                                                        status == StatusEnvio.CANCELADO,
                                                        pedido.getPrecoLivro(),
                                                        baseUrl));
                } catch (Exception e) {
                        log.error("ERRO_EMAIL_STATUS pedidoId={}", pedido.getId(), e);
                }

                try {
                        notificacaoService.criarNotificacaoDashboard(
                                        pedido.getComprador(),
                                        "Pedido #" + pedido.getId() + " atualizado para " + status.getDescricao(),
                                        "/clientes/homepage?aba=pedidos");
                } catch (Exception e) {
                        log.error("ERRO_NOTIFICACAO_STATUS pedidoId={}", pedido.getId(), e);
                }
        }
}