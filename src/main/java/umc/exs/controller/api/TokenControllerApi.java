package umc.exs.controller.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.CompraTokensRequestDTO;
import umc.exs.design.factory.PagamentoFactory;
import umc.exs.design.strategy.PagamentoStrategy;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.core.ClienteService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenControllerApi {

    private final ClienteService clienteService;
    private final PagamentoFactory pagamentoFactory;
    private final LogAuditoriaService logAuditoriaService;

    /**
     * Processa compra de tokens via API.
     * Suporta PIX (pendente) e cartão (imediato) com Strategy.
     * Registra log auditoria sucesso/falha.
     * 
     * @param userDetails usuário autenticado
     * @param request     DTO valor/método/cartão
     */
    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(

            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompraTokensRequestDTO request) {

        // 1. Verificação de Segurança
        if (userDetails == null) {
            log.error("Tentativa de compra sem usuário autenticado.");
            return ResponseEntity.status(401).body("Você precisa estar logado para comprar tokens.");
        }

        try {

            // 2. Buscar o cliente real no banco pelo e-mail do login
            String emailLogado = userDetails.getUsername();
            Cliente cliente = clienteService.buscarEntidadePorEmail(emailLogado)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado com o e-mail: " + emailLogado));

            log.info("Processando compra para o cliente: {} (ID: {})", cliente.getNome(), cliente.getId());

            // 3. Buscar Estratégia de Pagamento (PIX ou CARTAO)
            PagamentoStrategy estrategia = pagamentoFactory.buscarEstrategia(request.getMetodoPagamento());

            // 4. Processar o pagamento
            // Passamos o objeto request para que a strategy preencha os dados do PIX se
            // necessário
            boolean sucesso = estrategia.processar(request.getValor(), request);

            if (!sucesso) {
                logAuditoriaService.registrarLog("COMPRA_FALHA", cliente.getId(), emailLogado,
                        "Pagamento recusado pela operadora.");
                return ResponseEntity.badRequest().body("O pagamento não pôde ser processado.");
            }

            // 5. TRATAMENTO ESPECÍFICO POR MÉTODO
            if ("PIX".equalsIgnoreCase(request.getMetodoPagamento())) {
                clienteService.registrarTransacaoPendente(
                        cliente.getId(),
                        request.getValor(),
                        request.getPagamentoId());

                log.info("PIX gerado e registrado como PENDENTE para o cliente ID: {}", cliente.getId());
                return ResponseEntity.ok(request);
            }

            // 6. SE FOR CARTÃO (Aprovação imediata na simulação)
            clienteService.adicionarTokens(
                    cliente.getId(),
                    request.getValor(),
                    request.getMetodoPagamento(),
                    request.getNumeroCartao());

            logAuditoriaService.registrarLog("COMPRA_SUCESSO", cliente.getId(), emailLogado,
                    "Compra via Cartão: T$ " + request.getValor());

            // Retornamos o perfil atualizado para o JS atualizar o saldo na tela
            return ResponseEntity.ok(clienteService.buscarPorId(cliente.getId()));

        } catch (Exception e) {
            log.error("Erro crítico ao processar compra: ", e);
            return ResponseEntity.internalServerError().body("Erro interno ao processar sua compra. Tente novamente.");
        }
    }

    /**
     * Retorna histórico de transações de tokens do usuário autenticado.
     * Ordenado por data descendente.
     * Requer autenticação.
     * 
     * @param userDetails usuário logado para filtrar
     * @return List<Transacao> histórico
     */
    @GetMapping("/historico")
    public ResponseEntity<List<Transacao>> buscarHistorico(@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(clienteService.listarHistoricoTransacoes(userDetails.getUsername()));
    }

    /**
     * Verifica status de pagamento PIX pendente.
     * Retorna PENDENTE ou APROVADO.
     * Usado por frontend polling.
     * 
     * @param pagamentoId ID do pagamento
     */
    @GetMapping("/verificar-pagamento/{pagamentoId}")
    public ResponseEntity<?> verificarPagamento(@PathVariable String pagamentoId) {

        boolean pago = clienteService.verificarSeFoiPago(pagamentoId);

        if (pago) {
            return ResponseEntity.ok(Map.of("status", "APROVADO"));
        }
        return ResponseEntity.ok(Map.of("status", "PENDENTE"));
    }

    /**
     * Simula webhook de aprovação PIX para teste.
     * Chama aprovarPagamento no clienteService.
     * Útil para desenvolvimento local.
     * 
     * @param pagamentoId ID a aprovar
     */
    @GetMapping("/simular-webhook/{pagamentoId}")
    public ResponseEntity<?> simularWebhook(@PathVariable String pagamentoId) {

        log.info("--- SIMULAÇÃO DE WEBHOOK RECEBIDA: Pagamento ID {} ---", pagamentoId);

        try {
            clienteService.aprovarPagamento(pagamentoId);
            return ResponseEntity.ok(Map.of("mensagem", "Pagamento aprovado via simulação!"));
        } catch (Exception e) {
            log.error("Erro ao simular aprovação: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller REST API para compra de tokens.
 * Endpoints /api/tokens/comprar (PIX/cartão), /historico, verificar-pagamento,
 * simular-webhook.
 * Usa PagamentoFactory/Strategy, ClienteService, LogAuditoriaService.
 * Suporta autenticação JWT, retorna JSON status.
 */
