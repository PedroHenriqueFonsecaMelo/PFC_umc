package umc.exs.service.carteira;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class CarteiraService {

        private final TransacaoRepository transacaoRepository;
        private final ClienteRepository clienteRepository;
        private final LogAuditoriaService logAuditoriaService;
        private final EmailService emailService;
        private final NotificacaoService notificacaoService;

        private static final String STATUS_CONCLUIDO = "CONCLUIDO";
        private static final String STATUS_PENDENTE = "PENDENTE";
        private static final String ASSUNTO_EMAIL = "Atualização de saldo — Bibliotroca";
        private static final String METODO_PIX = "PIX";

        @Transactional
        public void adicionarTokens(Cliente cliente, Double valor, String metodo, String infoAdicional) {
                double saldoAnterior = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
                cliente.setSaldoTokens(saldoAnterior + valor);

                Transacao t = Transacao.builder()
                                .cliente(cliente)
                                .valor(valor)
                                .dataHora(LocalDateTime.now())
                                .metodoPagamento(metodo)
                                .status(STATUS_CONCLUIDO)
                                .finalCartao(infoAdicional)
                                .build();

                // Linha 53: Conversão segura para satisfazer @NonNull
                transacaoRepository.save(Objects.requireNonNull(t));
                clienteRepository.save(cliente);

                notificacaoService.notificarSaldo(cliente.getId(), cliente.getSaldoTokens(),
                                "Recarga de T$ " + String.format("%.2f", valor) + " via " + metodo);

                logAuditoriaService.registrarLog("TOKENS_ADICIONADOS", cliente.getId(), cliente.getEmail(),
                                String.format("Método: %s | Valor: T$%.2f | Info: %s", metodo, valor, infoAdicional));

                enviarEmailSaldo(cliente, saldoAnterior, valor, metodo, infoAdicional, true);
        }

        @SuppressWarnings("null")
        @Transactional
        public void debitarTokens(Cliente cliente, Double valor, String descricao) {
                double saldoAtual = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
                if (saldoAtual < valor) {
                        throw new IllegalArgumentException("Saldo insuficiente.");
                }
                cliente.setSaldoTokens(saldoAtual - valor);

                Transacao t = Transacao.builder()
                                .cliente(cliente)
                                .valor(-valor)
                                .dataHora(LocalDateTime.now())
                                .metodoPagamento("DEBITO_TOKENS")
                                .status(STATUS_CONCLUIDO)
                                .finalCartao(descricao)
                                .build();

                // Linha 86: Conversão segura
                transacaoRepository.save(Objects.requireNonNull(t));
                clienteRepository.save(cliente);

                notificacaoService.notificarSaldo(cliente.getId(), cliente.getSaldoTokens(),
                                "Débito de T$ " + String.format("%.2f", valor) + ": " + descricao);

                logAuditoriaService.registrarLog("TOKENS_DEBITADOS", cliente.getId(), cliente.getEmail(),
                                String.format("Valor: T$%.2f | Descrição: %s", valor, descricao));

                try {
                        // Linha 99-101: Conversão segura das Strings de e-mail e nome
                        emailService.enviarHtml(
                                        Objects.requireNonNull(cliente.getEmail()),
                                        Objects.requireNonNull(ASSUNTO_EMAIL),
                                        EmailHtmlBuilder.atualizacaoSaldo(
                                                        Objects.requireNonNull(cliente.getNome()),
                                                        saldoAtual, valor, cliente.getSaldoTokens(),
                                                        descricao, false, LocalDateTime.now()));
                } catch (Exception e) {
                        log.error("Erro no e-mail: {}", e.getMessage());
                }
        }

        @Transactional
        public void registrarIntencaoPagamento(Cliente cliente, Double valor, String pagamentoId) {
                Transacao t = Transacao.builder()
                                .cliente(cliente)
                                .valor(valor)
                                .pagamentoId(pagamentoId)
                                .status(STATUS_PENDENTE)
                                .metodoPagamento(METODO_PIX)
                                .dataHora(LocalDateTime.now())
                                .build();

                // Linha 124: Conversão segura
                transacaoRepository.save(Objects.requireNonNull(t));
        }

        @SuppressWarnings("null")
        @Transactional
        public void confirmarPagamentoPix(String pagamentoId) {
                Transacao transacao = transacaoRepository.findByPagamentoId(pagamentoId);
                if (transacao == null) {
                        throw new IllegalStateException("Transação não localizada: " + pagamentoId);
                }

                if (STATUS_PENDENTE.equals(transacao.getStatus())) {
                        transacao.setStatus(STATUS_CONCLUIDO);
                        Cliente cliente = Objects.requireNonNull(transacao.getCliente());
                        double saldoAnterior = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
                        double valorPix = transacao.getValor();
                        cliente.setSaldoTokens(saldoAnterior + valorPix);

                        transacaoRepository.save(transacao);
                        clienteRepository.save(cliente);

                        try {
                                // Linha 156-159: Conversão segura
                                emailService.enviarHtml(
                                                Objects.requireNonNull(cliente.getEmail()),
                                                Objects.requireNonNull(ASSUNTO_EMAIL),
                                                EmailHtmlBuilder.atualizacaoSaldo(
                                                                Objects.requireNonNull(cliente.getNome()),
                                                                saldoAnterior, valorPix, cliente.getSaldoTokens(),
                                                                "Recarga via PIX confirmada", true,
                                                                LocalDateTime.now()));
                        } catch (Exception e) {
                                log.error("Erro no e-mail PIX: {}", e.getMessage());
                        }
                }
        }

        @SuppressWarnings("null")
        private void enviarEmailSaldo(Cliente cliente, double saldoAnterior, Double valor, String metodo, String info,
                        boolean isCredito) {
                try {
                        String motivoEmail = switch (metodo.toUpperCase()) {
                                case METODO_PIX -> "Recarga via PIX";
                                case "CUPOM" -> "Resgate de cupom";
                                default -> "Crédito — " + metodo;
                        };
                        // Linha 183-185: Conversão segura
                        emailService.enviarHtml(
                                        Objects.requireNonNull(cliente.getEmail()),
                                        Objects.requireNonNull(ASSUNTO_EMAIL),
                                        EmailHtmlBuilder.atualizacaoSaldo(
                                                        Objects.requireNonNull(cliente.getNome()),
                                                        saldoAnterior, valor, cliente.getSaldoTokens(),
                                                        motivoEmail, isCredito, LocalDateTime.now()));
                } catch (Exception e) {
                        log.error("Erro e-mail saldo: {}", e.getMessage());
                }
        }

        @Transactional(readOnly = true)
        public List<Transacao> listarHistoricoPorCliente(Long clienteId) {
                return transacaoRepository.findByClienteIdOrderByDataHoraDesc(clienteId);
        }

        @Transactional(readOnly = true)
        public boolean verificarStatusPagamento(String pagamentoId) {
                Transacao t = transacaoRepository.findByPagamentoId(pagamentoId);
                return (t != null && STATUS_CONCLUIDO.equals(t.getStatus()));
        }
}