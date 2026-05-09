package umc.exs.service.carteira;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.TransacaoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

// LGPD Art. 16 — retenção obrigatória por 5 anos: nunca chamar delete() em Transacao.
@Slf4j
@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final TransacaoRepository transacaoRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoriaService;
    private final EmailService emailService;
    private final NotificacaoService notificacaoService;

    @SuppressWarnings("null")
    @Transactional
    public void adicionarTokens(Cliente cliente, Double valor, String metodo, String infoAdicional) {
        double saldoAnterior = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
        cliente.setSaldoTokens(saldoAnterior + valor);

        Transacao t = Transacao.builder()
                .cliente(cliente)
                .valor(valor)
                .dataHora(LocalDateTime.now())
                .metodoPagamento(metodo)
                .status("CONCLUIDO")
                .finalCartao(infoAdicional)
                .build();

        transacaoRepository.save(t);
        clienteRepository.save(cliente);

        // Notificação em tempo real via WebSocket
        notificacaoService.notificarSaldo(cliente.getId(), cliente.getSaldoTokens(),
                "Recarga de T$ " + String.format("%.2f", valor) + " via " + metodo);

        logAuditoriaService.registrarLog("TOKENS_ADICIONADOS", cliente.getId(), cliente.getEmail(),
                String.format("Método: %s | Valor: T$%.2f | Info: %s", metodo, valor, infoAdicional));

        log.info("Crédito de {} tokens via {} para cliente ID {}. Saldo: {} → {}",
                valor, metodo, cliente.getId(), saldoAnterior, cliente.getSaldoTokens());

        // E-mail de atualização de saldo ao cliente
        try {
            String motivoEmail = switch (metodo.toUpperCase()) {
                case "PIX"   -> "Recarga via PIX";
                case "CUPOM" -> "Resgate de cupom" +
                        (infoAdicional != null && !infoAdicional.isBlank() ? ": " + infoAdicional : "");
                case "ADMIN" -> "Crédito administrativo";
                default      -> "Crédito — " + metodo;
            };
            emailService.enviarHtml(
                    cliente.getEmail(),
                    "Atualização de saldo — Bibliotroca",
                    EmailHtmlBuilder.atualizacaoSaldo(
                            cliente.getNome(), saldoAnterior, valor,
                            cliente.getSaldoTokens(), motivoEmail, true, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de atualização de saldo para {}: {}", cliente.getEmail(), e.getMessage());
        }
    }

    /**
     * Debita tokens do saldo do cliente.
     * Lança {@link IllegalArgumentException} se saldo insuficiente.
     */
    @Transactional
    public void debitarTokens(Cliente cliente, Double valor, String descricao) {
        double saldoAtual = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
        if (saldoAtual < valor) {
            throw new IllegalArgumentException("Saldo insuficiente. Saldo atual: T$ " +
                    String.format("%.2f", saldoAtual) + ", necessário: T$ " + String.format("%.2f", valor));
        }
        cliente.setSaldoTokens(saldoAtual - valor);

        Transacao t = Transacao.builder()
                .cliente(cliente)
                .valor(-valor)
                .dataHora(LocalDateTime.now())
                .metodoPagamento("DEBITO_TOKENS")
                .status("CONCLUIDO")
                .finalCartao(descricao)
                .build();

        transacaoRepository.save(t);
        clienteRepository.save(cliente);

        notificacaoService.notificarSaldo(cliente.getId(), cliente.getSaldoTokens(),
                "Débito de T$ " + String.format("%.2f", valor) + ": " + descricao);

        logAuditoriaService.registrarLog("TOKENS_DEBITADOS", cliente.getId(), cliente.getEmail(),
                String.format("Valor: T$%.2f | Descrição: %s", valor, descricao));

        // E-mail de atualização de saldo (débito)
        try {
            emailService.enviarHtml(
                    cliente.getEmail(),
                    "Atualização de saldo — Bibliotroca",
                    EmailHtmlBuilder.atualizacaoSaldo(
                            cliente.getNome(), saldoAtual, valor,
                            cliente.getSaldoTokens(), descricao, false, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de débito de tokens para {}: {}", cliente.getEmail(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Transacao> listarHistoricoPorCliente(Long clienteId) {
        log.debug("Buscando histórico de transações para o cliente ID: {}", clienteId);
        return transacaoRepository.findByClienteIdOrderByDataHoraDesc(clienteId);
    }

    @SuppressWarnings("null")
    @Transactional
    public void registrarIntencaoPagamento(Cliente cliente, Double valor, String pagamentoId) {
        Transacao t = Transacao.builder()
                .cliente(cliente)
                .valor(valor)
                .pagamentoId(pagamentoId)
                .status("PENDENTE")
                .metodoPagamento("PIX")
                .dataHora(LocalDateTime.now())
                .build();

        transacaoRepository.save(t);
        log.info("Intenção PIX registrada. PagamentoId: {} | Cliente: {} | Valor: {}",
                pagamentoId, cliente.getEmail(), valor);
    }

    @Transactional
    public void confirmarPagamentoPix(String pagamentoId) {
        Transacao transacao = transacaoRepository.findByPagamentoId(pagamentoId);

        if (transacao == null) {
            throw new RuntimeException("Transação não localizada para o pagamento: " + pagamentoId);
        }

        if ("PENDENTE".equals(transacao.getStatus())) {
            transacao.setStatus("CONCLUIDO");
            Cliente cliente = transacao.getCliente();
            double saldoAnterior = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
            double valorPix = transacao.getValor();
            cliente.setSaldoTokens(saldoAnterior + valorPix);

            transacaoRepository.save(transacao);
            clienteRepository.save(cliente);

            logAuditoriaService.registrarLog("TOKENS_PIX_SUCESSO", cliente.getId(), cliente.getEmail(),
                    String.format("PagamentoId: %s | Valor: T$%.2f", pagamentoId, valorPix));

            log.info("PIX confirmado! PagamentoId: {} | Cliente: {} | Tokens: {}",
                    pagamentoId, cliente.getEmail(), valorPix);

            // E-mail de atualização de saldo (PIX confirmado)
            try {
                emailService.enviarHtml(
                        cliente.getEmail(),
                        "Atualização de saldo — Bibliotroca",
                        EmailHtmlBuilder.atualizacaoSaldo(
                                cliente.getNome(), saldoAnterior, valorPix,
                                cliente.getSaldoTokens(), "Recarga via PIX confirmada", true,
                                LocalDateTime.now()));
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de PIX confirmado para {}: {}", cliente.getEmail(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean verificarStatusPagamento(String pagamentoId) {
        Transacao t = transacaoRepository.findByPagamentoId(pagamentoId);
        return (t != null && "CONCLUIDO".equals(t.getStatus()));
    }
}
