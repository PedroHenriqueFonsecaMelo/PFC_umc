package umc.exs.controller.api.compras;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.CompraTokensRequestDTO;
import umc.exs.design.factory.PagamentoFactory;
import umc.exs.design.strategy.PagamentoStrategy;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenControllerApi {

    // SEMPRE use o ClienteService (Orquestrador) em controllers
    private final ClienteService clienteService;
    private final PagamentoFactory pagamentoFactory;
    private final LogAuditoriaService logAuditoriaService;

    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompraTokensRequestDTO request) {

        if (userDetails == null) {
            log.error("Tentativa de compra sem usuário autenticado.");
            return ResponseEntity.status(401).body("Você precisa estar logado para comprar tokens.");
        }

        try {
            String emailLogado = userDetails.getUsername();
            
            // Ajustado para usar o método que retorna a entidade para lógica interna
            Cliente cliente = clienteService.buscarEntidadePorEmail(emailLogado);

            log.info("Processando compra para o cliente: {} (ID: {})", cliente.getNome(), cliente.getId());

            PagamentoStrategy estrategia = pagamentoFactory.buscarEstrategia(request.getMetodoPagamento());
            boolean sucesso = estrategia.processar(request.getValor(), request);

            if (!sucesso) {
                logAuditoriaService.registrarLog("COMPRA_FALHA", cliente.getId(), emailLogado,
                        "Pagamento recusado pela operadora.");
                return ResponseEntity.badRequest().body("O pagamento não pôde ser processado.");
            }

            // TRATAMENTO ESPECÍFICO POR MÉTODO
            if ("PIX".equalsIgnoreCase(request.getMetodoPagamento())) {
                clienteService.registrarTransacaoPendente(
                        cliente.getId(),
                        request.getValor(),
                        request.getPagamentoId());

                log.info("PIX gerado e registrado como PENDENTE para o cliente ID: {}", cliente.getId());
                return ResponseEntity.ok(request);
            }

            // SE FOR CARTÃO (Aprovação imediata)
            clienteService.adicionarTokens(
                    cliente.getId(),
                    request.getValor(),
                    request.getMetodoPagamento(),
                    request.getNumeroCartao());

            // O log de "COMPRA_SUCESSO" já pode estar dentro do clienteService.adicionarTokens 
            // ou mantido aqui se quiser um log específico do Controller.

            return ResponseEntity.ok(clienteService.buscarPorId(cliente.getId()));

        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação na compra: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro crítico ao processar compra: ", e);
            return ResponseEntity.internalServerError().body("Erro interno ao processar sua compra.");
        }
    }

    @GetMapping("/historico")
    public ResponseEntity<List<Transacao>> buscarHistorico(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        
        // Chamando o orquestrador
        return ResponseEntity.ok(clienteService.listarHistoricoTransacoes(userDetails.getUsername()));
    }

    @GetMapping("/verificar-pagamento/{pagamentoId}")
    public ResponseEntity<?> verificarPagamento(@PathVariable String pagamentoId) {
        boolean pago = clienteService.verificarSeFoiPago(pagamentoId);
        String status = pago ? "APROVADO" : "PENDENTE";
        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/simular-webhook/{pagamentoId}")
    public ResponseEntity<?> simularWebhook(@PathVariable String pagamentoId) {
        log.info("--- SIMULAÇÃO DE WEBHOOK: Pagamento ID {} ---", pagamentoId);
        try {
            clienteService.aprovarPagamento(pagamentoId);
            return ResponseEntity.ok(Map.of("mensagem", "Pagamento aprovado via simulação!"));
        } catch (Exception e) {
            log.error("Erro ao simular aprovação: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}