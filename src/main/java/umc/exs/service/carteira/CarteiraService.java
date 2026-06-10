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
import umc.exs.service.carteira.delegado.CarteiraEmailService;
import umc.exs.service.carteira.delegado.CarteiraNotificacaoService;
import umc.exs.service.cliente.delegado.ClienteRepositoryService;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarteiraService {

        private final TransacaoRepository transacaoRepository;
        private final ClienteRepositoryService clienteRepositoryService;
        private final CarteiraEmailService carteiraEmailService;
        private final CarteiraNotificacaoService carteiraNotificacaoService;
        private final LogAuditoriaService auditoria;

        private static final String STATUS_CONCLUIDO = "CONCLUIDO";
        private static final String STATUS_PENDENTE = "PENDENTE";

        @Transactional
        public void adicionarTokens(
                        Cliente cliente,
                        Double valor,
                        String metodo,
                        String infoAdicional) {

                double saldoAnterior = saldo(cliente);

                transacaoRepository.save(
                                Transacao.criarTransacao(cliente, valor, metodo, STATUS_CONCLUIDO, infoAdicional));

                carteiraEmailService.enviarCredito(cliente, saldoAnterior, valor, metodo);

                cliente.setSaldoTokens(saldoAnterior + valor);
                clienteRepositoryService.salvar(cliente);

                carteiraNotificacaoService.notificarRecarga(cliente, valor, metodo);

                carteiraEmailService.enviarCredito(cliente, saldoAnterior, valor, metodo);

                cliente.setSaldoTokens(saldoAnterior + valor);
                clienteRepositoryService.salvar(cliente);

                log.info("Tokens adicionados clienteId={} valor={} metodo={}",
                                cliente.getId(), valor, metodo);
        }

        @Transactional
        public void debitarTokens(
                        Cliente cliente,
                        Double valor,
                        String descricao) {

                double saldoAtual = saldo(cliente);

                if (saldoAtual < valor) {
                        log.warn("Tentativa de débito sem saldo clienteId={} saldo={} valor={}",
                                        cliente.getId(), saldoAtual, valor);

                        throw new IllegalArgumentException("Saldo insuficiente.");
                }

                cliente.setSaldoTokens(saldoAtual - valor);

                transacaoRepository.save(
                                Transacao.criarTransacao(
                                                cliente,
                                                -valor,
                                                "DEBITO_TOKENS",
                                                STATUS_CONCLUIDO,
                                                descricao));

                clienteRepositoryService.salvar(cliente);

                carteiraNotificacaoService.notificarDebito(
                                cliente,
                                valor,
                                descricao);

                auditoria.registrarLog(
                                AcaoAuditoria.CARTEIRA_TOKEN_DEBITADO.name(),
                                String.format("Valor: T$%.2f | Descrição: %s", valor, descricao));

                carteiraEmailService.enviarDebito(
                                cliente,
                                saldoAtual,
                                valor,
                                descricao);

                log.info("Tokens debitados clienteId={} valor={} descricao={}",
                                cliente.getId(), valor, descricao);
        }

        @Transactional
        public void registrarIntencaoPagamento(
                        Cliente cliente,
                        Double valor,
                        String pagamentoId) {

                Transacao transacao = Transacao.builder()
                                .cliente(cliente)
                                .valor(valor)
                                .pagamentoId(pagamentoId)
                                .status(STATUS_PENDENTE)
                                .metodoPagamento("PIX")
                                .dataHora(LocalDateTime.now())
                                .build();

                transacaoRepository.save(transacao);

                auditoria.registrarLog(
                                AcaoAuditoria.PAGAMENTO_INTENCAO_REGISTRADA.name(),
                                String.format("PIX criado | Valor: %.2f | PagamentoId: %s",
                                                valor, pagamentoId));

                log.info("Intenção de pagamento registrada clienteId={} pagamentoId={}",
                                cliente.getId(), pagamentoId);
        }

        @Transactional
        public void confirmarPagamentoPix(String pagamentoId) {

                Transacao transacao = transacaoRepository.findByPagamentoId(pagamentoId);

                if (transacao == null) {
                        log.error("Pagamento não encontrado pagamentoId={}", pagamentoId);
                        throw new IllegalStateException("Transação não localizada: " + pagamentoId);
                }

                if (!STATUS_PENDENTE.equals(transacao.getStatus())) {
                        log.warn("Pagamento já processado pagamentoId={} status={}",
                                        pagamentoId, transacao.getStatus());
                        return;
                }

                Cliente cliente = transacao.getCliente();
                double saldoAnterior = saldo(cliente);

                transacao.setStatus(STATUS_CONCLUIDO);
                transacaoRepository.save(transacao);

                carteiraEmailService.enviarConfirmacaoPix(cliente, saldoAnterior, transacao.getValor());

                cliente.setSaldoTokens(saldoAnterior + transacao.getValor());
                clienteRepositoryService.salvar(cliente);

                carteiraNotificacaoService.notificarPixConfirmado(cliente, transacao.getValor());
                auditoria.registrarLog(
                                AcaoAuditoria.PAGAMENTO_PIX_CONFIRMADO.name(),
                                String.format("PIX confirmado | Valor: %.2f | PagamentoId: %s",
                                                transacao.getValor(), pagamentoId));

                log.info("PIX confirmado clienteId={} valor={} pagamentoId={}",
                                cliente.getId(), transacao.getValor(), pagamentoId);
        }

        @Transactional(readOnly = true)
        public List<Transacao> listarHistoricoPorCliente(Long clienteId) {
                return transacaoRepository
                                .findByClienteIdAndStatusOrderByDataHoraDesc(
                                                clienteId,
                                                STATUS_CONCLUIDO);
        }

        @Transactional(readOnly = true)
        public boolean verificarStatusPagamento(String pagamentoId) {

                Transacao transacao = transacaoRepository.findByPagamentoId(pagamentoId);

                return transacao != null
                                && STATUS_CONCLUIDO.equals(transacao.getStatus());
        }

        private double saldo(Cliente cliente) {
                return cliente.getSaldoTokens() == null ? 0.0 : cliente.getSaldoTokens();
        }
}