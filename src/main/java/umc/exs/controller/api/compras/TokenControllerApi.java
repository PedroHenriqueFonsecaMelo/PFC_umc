package umc.exs.controller.api.compras;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.CompraTokensRequestDTO;
import umc.exs.design.strategy.impl.PagamentoPixStrategy;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenControllerApi {

    private static final double TOKENS_POR_REAL = 2.0;

    private final ClienteService clienteService;
    private final PagamentoPixStrategy pixStrategy;
    private final LogAuditoriaService logAuditoriaService;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostMapping("/comprar")
    public ResponseEntity<?> comprar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompraTokensRequestDTO request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Você precisa estar logado.");
        }

        try {
            Cliente cliente = clienteService.buscarEntidadePorEmail(userDetails.getUsername());

            // Injeta o e-mail do pagador (necessário para o MP)
            request.setEmailPagador(cliente.getEmail());

            boolean sucesso = pixStrategy.processar(request.getValor(), request);

            if (!sucesso) {
                logAuditoriaService.registrarLog("PIX_FALHA", cliente.getId(),
                        cliente.getEmail(), "Geração de PIX falhou.");
                return ResponseEntity.badRequest().body("Não foi possível gerar o PIX. Tente novamente.");
            }

            // Armazena o valor em TOKENS (R$ × 2) na transação pendente
            double tokens = request.getValor() * TOKENS_POR_REAL;
            clienteService.registrarTransacaoPendente(
                    cliente.getId(), tokens, request.getPagamentoId());

            log.info("PIX pendente — cliente {} | R$ {} → T$ {} | ID {}",
                    cliente.getEmail(), request.getValor(), tokens, request.getPagamentoId());

            return ResponseEntity.ok(request);

        } catch (Exception e) {
            log.error("Erro ao processar compra PIX: ", e);
            return ResponseEntity.internalServerError().body("Erro interno ao gerar o PIX.");
        }
    }

    @GetMapping("/historico")
    public ResponseEntity<List<Transacao>> buscarHistorico(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(clienteService.listarHistoricoTransacoes(userDetails.getUsername()));
    }

    @GetMapping("/verificar-pagamento/{pagamentoId}")
    public ResponseEntity<?> verificarPagamento(@PathVariable String pagamentoId) {
        boolean pago = clienteService.verificarSeFoiPago(pagamentoId);

        // Para pagamentos reais do MP ainda pendentes: consulta a API diretamente.
        // Isso elimina a necessidade de webhook em ambiente de desenvolvimento.
        if (!pago && !pagamentoId.startsWith("SIM-")) {
            try {
                MercadoPagoConfig.setAccessToken(accessToken);
                PaymentClient mpClient = new PaymentClient();
                Payment payment = mpClient.get(Long.parseLong(pagamentoId));

                if ("approved".equals(payment.getStatus())) {
                    clienteService.aprovarPagamento(pagamentoId);
                    pago = true;
                    log.info("Polling MP: pagamento {} aprovado — tokens creditados.", pagamentoId);
                }
            } catch (Exception e) {
                log.debug("Polling MP para {}: {}", pagamentoId, e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("status", pago ? "APROVADO" : "PENDENTE"));
    }

    // ── Webhook do Mercado Pago ──────────────────────────────────────
    // Configure esta URL no painel do MP: https://seu-dominio.com/api/tokens/webhook
    @PostMapping("/webhook")
    public ResponseEntity<?> webhookMercadoPago(@RequestBody Map<String, Object> body) {
        try {
            String type = (String) body.get("type");
            if (!"payment".equals(type)) {
                return ResponseEntity.ok().build();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            String paymentId = String.valueOf(data.get("id"));

            MercadoPagoConfig.setAccessToken(accessToken);
            PaymentClient mpClient = new PaymentClient();
            Payment payment = mpClient.get(Long.parseLong(paymentId));

            if ("approved".equals(payment.getStatus())) {
                clienteService.aprovarPagamento(paymentId);
                log.info("Webhook MP: pagamento {} aprovado — tokens creditados.", paymentId);
            } else {
                log.info("Webhook MP: pagamento {} com status {}", paymentId, payment.getStatus());
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago: {}", e.getMessage());
            return ResponseEntity.ok().build(); // sempre 200 para o MP não reenviar
        }
    }

    // Mantido apenas para testes em desenvolvimento (sem webhook acessível)
    @GetMapping("/simular-webhook/{pagamentoId}")
    public ResponseEntity<?> simularWebhook(@PathVariable String pagamentoId) {
        log.info("Simulação de aprovação PIX: {}", pagamentoId);
        try {
            clienteService.aprovarPagamento(pagamentoId);
            return ResponseEntity.ok(Map.of("mensagem", "Pagamento aprovado via simulação!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
