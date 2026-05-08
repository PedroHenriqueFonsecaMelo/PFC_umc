package umc.exs.service.core.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.PedidoDTO;
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
        public List<PedidoDTO> listarTodos() {
                return pedidoRepository.findAll(
                                org.springframework.data.domain.Sort.by(
                                                org.springframework.data.domain.Sort.Direction.DESC, "dataCompra"))
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        /** Todos os pedidos do cliente, mais recente primeiro. */
        @Transactional(readOnly = true)
        public List<PedidoDTO> listarPorCliente(Long compradorId) {
                return pedidoRepository
                                .findByCompradorIdOrderByDataCompraDesc(compradorId)
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        /** Pedidos com status AGUARDANDO_ENVIO ou EM_TRANSITO (pendentes). */
        @Transactional(readOnly = true)
        public List<PedidoDTO> listarPendentes(Long compradorId) {
                return pedidoRepository
                                .findByCompradorIdAndStatusEnvioNotInOrderByDataCompraDesc(
                                                compradorId,
                                                List.of(StatusEnvio.ENTREGUE, StatusEnvio.CANCELADO))
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        /** Pedidos com status ENTREGUE (concluídos). */
        @Transactional(readOnly = true)
        public List<PedidoDTO> listarConcluidos(Long compradorId) {
                return pedidoRepository
                                .findByCompradorIdAndStatusEnvioOrderByDataCompraDesc(
                                                compradorId, StatusEnvio.ENTREGUE)
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        // ==========================================================
        // ATUALIZAÇÃO DE STATUS (admin ou sistema)
        // ==========================================================

        @SuppressWarnings("null")
        @Transactional
        public PedidoDTO atualizarStatus(Long pedidoId, StatusEnvio novoStatus, String codigoRastreio) {
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
                                                true));
                        } catch (Exception e) {
                                log.error("Falha ao enviar e-mail de estorno para {}: {}", comprador.getEmail(), e.getMessage());
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
                }

                return toDTO(salvo);
        }

        // ==========================================================
        // MAPPER INTERNO
        // ==========================================================

        private PedidoDTO toDTO(Pedido p) {
                Double saldoApos = null;
                String enderecoFormatado = null;

                if (p.getComprador() != null) {
                        if (p.getStatusEnvio() == StatusEnvio.CANCELADO) {
                                saldoApos = p.getComprador().getSaldoTokens();
                        }
                        enderecoFormatado = p.getComprador().getEnderecos().stream()
                                        .findFirst()
                                        .map(e -> {
                                                StringBuilder sb = new StringBuilder();
                                                if (e.getRua()    != null) sb.append(e.getRua());
                                                if (e.getNumero() != null) sb.append(", ").append(e.getNumero());
                                                if (e.getComplemento() != null && !e.getComplemento().isBlank())
                                                        sb.append(" - ").append(e.getComplemento());
                                                if (e.getBairro() != null) sb.append(", ").append(e.getBairro());
                                                if (e.getCidade() != null) sb.append(", ").append(e.getCidade());
                                                if (e.getEstado() != null) sb.append(" - ").append(e.getEstado());
                                                if (e.getCep()    != null) sb.append(" · CEP: ").append(e.getCep());
                                                return sb.toString();
                                        })
                                        .orElse(null);
                }

                return PedidoDTO.builder()
                                .id(p.getId())
                                .livroId(p.getLivroId())
                                .tituloLivro(p.getTituloLivro())
                                .autorLivro(p.getAutorLivro())
                                .isbnLivro(p.getIsbnLivro())
                                .fotosUrls(p.getFotosUrls())
                                .precoLivro(p.getPrecoLivro())
                                .statusEnvio(p.getStatusEnvio())
                                .statusEnvioDescricao(p.getStatusEnvio().getDescricao())
                                .dataCompra(p.getDataCompra())
                                .dataAtualizacaoStatus(p.getDataAtualizacaoStatus())
                                .codigoRastreio(p.getCodigoRastreio())
                                .compradorNome(p.getComprador() != null ? p.getComprador().getNome() : null)
                                .compradorEmail(p.getComprador() != null ? p.getComprador().getEmail() : null)
                                .compradorEndereco(enderecoFormatado)
                                .saldoAposEstorno(saldoApos)
                                .build();
        }
}
