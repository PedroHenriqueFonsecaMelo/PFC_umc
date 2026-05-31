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

                cliente.setSaldoTokens(saldoAnterior + valor);

                transacaoRepository.save(
                                Transacao.criarTransacao(
                                                cliente,
                                                valor,
                                                metodo,
                                                STATUS_CONCLUIDO,
                                                infoAdicional));

                clienteRepositoryService.salvar(cliente);

                carteiraNotificacaoService.notificarRecarga(
                                cliente,
                                valor,
                                metodo);

                auditoria.registrarLog(
                                "TOKENS_ADICIONADOS",
                                cliente.getId(),
                                cliente.getEmail(),
                                String.format(
                                                "Método: %s | Valor: T$%.2f",
                                                metodo,
                                                valor));

                carteiraEmailService.enviarCredito(
                                cliente,
                                saldoAnterior,
                                valor,
                                metodo);
        }

        @Transactional
        public void debitarTokens(
                        Cliente cliente,
                        Double valor,
                        String descricao) {

                double saldoAtual = saldo(cliente);

                if (saldoAtual < valor) {
                        throw new IllegalArgumentException(
                                        "Saldo insuficiente.");
                }

                cliente.setSaldoTokens(
                                saldoAtual - valor);

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
                                "TOKENS_DEBITADOS",
                                cliente.getId(),
                                cliente.getEmail(),
                                String.format(
                                                "Valor: T$%.2f | Descrição: %s",
                                                valor,
                                                descricao));

                carteiraEmailService.enviarDebito(
                                cliente,
                                saldoAtual,
                                valor,
                                descricao);
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
        }

        @Transactional
        public void confirmarPagamentoPix(
                        String pagamentoId) {

                Transacao transacao = transacaoRepository.findByPagamentoId(pagamentoId);

                if (transacao == null) {
                        throw new IllegalStateException(
                                        "Transação não localizada: " + pagamentoId);
                }

                if (!STATUS_PENDENTE.equals(
                                transacao.getStatus())) {
                        return;
                }

                Cliente cliente = transacao.getCliente();

                double saldoAnterior = saldo(cliente);

                transacao.setStatus(STATUS_CONCLUIDO);

                cliente.setSaldoTokens(
                                saldoAnterior + transacao.getValor());

                transacaoRepository.save(transacao);
                clienteRepositoryService.salvar(cliente);

                carteiraEmailService.enviarConfirmacaoPix(
                                cliente,
                                saldoAnterior,
                                transacao.getValor());

                carteiraNotificacaoService.notificarPixConfirmado(
                                cliente,
                                transacao.getValor());
        }

        @Transactional(readOnly = true)
        public List<Transacao> listarHistoricoPorCliente(
                        Long clienteId) {

                return transacaoRepository
                                .findByClienteIdOrderByDataHoraDesc(clienteId);
        }

        @Transactional(readOnly = true)
        public boolean verificarStatusPagamento(
                        String pagamentoId) {

                Transacao transacao = transacaoRepository.findByPagamentoId(pagamentoId);

                return transacao != null
                                && STATUS_CONCLUIDO.equals(
                                                transacao.getStatus());
        }

        private double saldo(Cliente cliente) {
                return cliente.getSaldoTokens() == null
                                ? 0.0
                                : cliente.getSaldoTokens();
        }
}