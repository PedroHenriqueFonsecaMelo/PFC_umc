package umc.exs.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.design.factory.PagamentoFactory;
import umc.exs.design.strategy.PagamentoStrategy;
import umc.exs.log.LogAuditoriaService;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.CompraTokensRequestDTO;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.ClienteService;

/**
 * REST Controller para operações financeiras de tokens.
 * Retorna dados em JSON para o JavaScript da Carteira.
 */
@Slf4j
@RestController
@RequestMapping("/clientes/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final ClienteService clienteService;
    private final PagamentoFactory pagamentoFactory;
    private final LogAuditoriaService logAuditoriaService;

    /**
     * Endpoint POST para processar a compra de tokens.
     */
    @PostMapping("/comprar")
    public ResponseEntity<?> comprarTokens(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CompraTokensRequestDTO request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Usuário não autenticado.");
        }

        if (request == null || request.getValor() == null || request.getValor() <= 0) {
            return ResponseEntity.badRequest().body("Dados de compra inválidos.");
        }

        String email = userDetails.getUsername();
        
        // Busca a entidade para ter acesso ao ID e persistência
        Cliente cliente = clienteService.buscarEntidadePorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado no sistema."));

        try {
            // 1. Resolve a estratégia de pagamento (Cartão ou PIX) via Design Pattern Factory
            PagamentoStrategy estrategia = pagamentoFactory.buscarEstrategia(request.getMetodoPagamento());
            
            // 2. Processa o pagamento (simulação ou gateway real)
            boolean sucesso = estrategia.processar(request.getValor(), request);

            if (!sucesso) {
                logAuditoriaService.registrarLog("COMPRA_TOKEN_FALHA", cliente.getId(), email, "Pagamento recusado pelo método: " + request.getMetodoPagamento());
                return ResponseEntity.badRequest().body("Pagamento recusado. Verifique os dados do cartão.");
            }

            // 3. Atualiza o saldo no banco de dados e gera o histórico de Transação
            ClienteDTO atualizado = clienteService.adicionarTokens(
                    cliente.getId(),
                    request.getValor(),
                    request.getMetodoPagamento(),
                    request.getNumeroCartao());

            // 4. Auditoria
            logAuditoriaService.registrarLog("COMPRA_TOKEN_SUCESSO", cliente.getId(), email,
                    String.format("Compra aprovada: T$ %.2f via %s", request.getValor(), request.getMetodoPagamento()));

            return ResponseEntity.ok(atualizado);

        } catch (IllegalArgumentException e) {
            log.warn("Método de pagamento inválido para {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body("Método de pagamento não suportado.");
        } catch (Exception e) {
            log.error("Erro crítico ao processar compra para {}: {}", email, e.getMessage());
            logAuditoriaService.registrarLog("COMPRA_TOKEN_ERRO", cliente.getId(), email, "Erro interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Ocorreu um erro no processamento. Tente novamente mais tarde.");
        }
    }

    /**
     * Endpoint GET que retorna a lista de transações do usuário logado.
     * Alimenta a tabela dinâmica no HTML da carteira.
     */
    @GetMapping("/historico")
    public ResponseEntity<List<Transacao>> buscarHistorico(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        log.info("Buscando histórico de tokens para: {}", userDetails.getUsername());
        List<Transacao> historico = clienteService.listarHistoricoTransacoes(userDetails.getUsername());
        return ResponseEntity.ok(historico);
    }
}