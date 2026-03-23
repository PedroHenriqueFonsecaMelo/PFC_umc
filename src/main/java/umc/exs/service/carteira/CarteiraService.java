package umc.exs.service.carteira;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.TransacaoRepository;
import umc.exs.service.log.LogAuditoriaService;

/**
 * Responsabilidade: Gestão de saldo (Tokens), histórico de transações,
 * processamento de pagamentos (Pix/Cartão) e reconciliação financeira.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarteiraService {

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    private final TransacaoRepository transacaoRepository;
    private final ClienteRepository clienteRepository;

    // ==========================================================
    // 🪙 GESTÃO DE SALDO E TRANSAÇÕES
    // ==========================================================

/**
 * Adiciona tokens saldo cliente e registra transação.
 * Para pagamentos instantâneos (cartão).
 * Log audit + info console após save.
 */

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
                .finalCartao(infoAdicional) // Pode ser os 4 dígitos do cartão ou ID externo
                .build();

        transacaoRepository.save(t);
        clienteRepository.save(cliente);

        logAuditoriaService.registrarLog("TOKENS_ADICIONADOS", cliente.getId(), cliente.getEmail(),
                String.format("Método: %s | Valor: T$%.2f | Cartão: %s", metodo, valor, infoAdicional));
        
        log.info("Crédito de {} tokens realizado via {} para o cliente ID: {}. Saldo anterior: {} | Novo saldo: {}", 
                 valor, metodo, cliente.getId(), saldoAnterior, cliente.getSaldoTokens());
    }

/**
 * Lista histórico transações cliente data desc recente.
 * Read-only transacional.
 * Log debug busca.
 */

    @Transactional(readOnly = true)
    public List<Transacao> listarHistoricoPorCliente(Long clienteId) {
        log.debug("Buscando histórico de transações para o cliente ID: {}", clienteId);
        return transacaoRepository.findByClienteIdOrderByDataHoraDesc(clienteId);
    }

    // ==========================================================
    // 📱 LÓGICA DE PAGAMENTO (PIX / PENDENTES)
    // ==========================================================

    /**
     * Registra uma transação com status PENDENTE. Utilizado para fluxos onde 
     * a aprovação depende de um webhook ou polling externo (como PIX).
     */
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
        log.info("Intenção de pagamento PIX registrada. PagamentoId: {} | Cliente: {} | Valor: {}", 
                 pagamentoId, cliente.getEmail(), valor);
    }

    /**
     * Confirma um pagamento previamente pendente, atualizando o saldo do cliente.
     */
    @Transactional
    public void confirmarPagamentoPix(String pagamentoId) {
        Transacao transacao = transacaoRepository.findByPagamentoId(pagamentoId);
        
        if (transacao == null) {
            log.error("Tentativa de confirmação falhou: PagamentoId {} não encontrado.", pagamentoId);
            throw new RuntimeException("Transação não localizada para o pagamento: " + pagamentoId);
        }

        if ("PENDENTE".equals(transacao.getStatus())) {
            transacao.setStatus("CONCLUIDO");
            
            Cliente cliente = transacao.getCliente();
            double saldoAtual = (cliente.getSaldoTokens() != null) ? cliente.getSaldoTokens() : 0.0;
            cliente.setSaldoTokens(saldoAtual + transacao.getValor());

            transacaoRepository.save(transacao);
            clienteRepository.save(cliente);

            logAuditoriaService.registrarLog("TOKENS_PIX_SUCESSO", cliente.getId(), cliente.getEmail(),
                    String.format("PagamentoId: %s | Valor: T$%.2f", pagamentoId, transacao.getValor()));
            
            log.info("Pagamento PIX confirmado com sucesso! PagamentoId: {} | Cliente: {} | Tokens adicionados: {}", 
                     pagamentoId, cliente.getEmail(), transacao.getValor());
        } else {
            log.warn("Tentativa de confirmar pagamento PIX {} ignorada: Status atual é {}", 
                     pagamentoId, transacao.getStatus());
        }
    }

    /**
     * Verifica se um pagamento específico já foi concluído.
     */
    @Transactional(readOnly = true)
    public boolean verificarStatusPagamento(String pagamentoId) {
        Transacao t = transacaoRepository.findByPagamentoId(pagamentoId);
        boolean concluido = (t != null && "CONCLUIDO".equals(t.getStatus()));
        
        log.debug("Verificação de status para PagamentoId {}: {}", pagamentoId, concluido ? "PAGO" : "NÃO PAGO/PENDENTE");
        return concluido;
    }
}