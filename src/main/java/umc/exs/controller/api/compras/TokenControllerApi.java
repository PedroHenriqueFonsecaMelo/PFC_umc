package umc.exs.controller.api.compras;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
import umc.exs.design.strategy.impl.PagamentoPixStrategy;
import umc.exs.dto.compra.CompraTokensRequestDTO;
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
    public ResponseEntity<CompraTokensRequestDTO> comprar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompraTokensRequestDTO request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Cliente cliente = clienteService.buscarEntidadePorEmail(userDetails.getUsername());

            request.setEmailPagador(cliente.getEmail());

            boolean sucesso = pixStrategy.processar(request.getValor(), request);

            if (!sucesso) {
                logAuditoriaService.registrarLog("PIX_FALHA", cliente.getId(),
                        cliente.getEmail(), "Geração de PIX falhou.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            double tokens = request.getValor() * TOKENS_POR_REAL;

            clienteService.registrarTransacaoPendente(
                    cliente.getId(), tokens, request.getPagamentoId());

            log.info("PIX pendente — cliente {} | R$ {} → T$ {} | ID {}",
                    cliente.getEmail(), request.getValor(), tokens, request.getPagamentoId());

            return ResponseEntity.status(HttpStatus.CREATED).body(request);

        } catch (Exception e) {
            log.error("Erro ao processar compra PIX: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historico")
    public ResponseEntity<List<Transacao>> buscarHistorico(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Transacao> historico =
                clienteService.listarHistoricoTransacoes(userDetails.getUsername());

        return ResponseEntity.ok(historico);
    }

    @GetMapping("/verificar-pagamento/{pagamentoId}")
    public ResponseEntity<Map<String, String>> verificarPagamento(
            @PathVariable String pagamentoId) {

        boolean pago = clienteService.verificarSeFoiPago(pagamentoId);

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

        return ResponseEntity.ok(
                Map.of("status", pago ? "APROVADO" : "PENDENTE"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhookMercadoPago(
            @RequestBody Map<String, Object> body) {

        try {
            String type = (String) body.get("type");

            if (!"payment".equals(type)) {
                return ResponseEntity.ok().build();
            }

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
            return ResponseEntity.ok().build();
        }
    }

    @GetMapping("/simular-webhook/{pagamentoId}")
    public ResponseEntity<Map<String, String>> simularWebhook(
            @PathVariable String pagamentoId) {

        log.info("Simulação de aprovação PIX: {}", pagamentoId);

        try {
            clienteService.aprovarPagamento(pagamentoId);

            return ResponseEntity.ok(
                    Map.of("mensagem", "Pagamento aprovado via simulação!"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", e.getMessage()));
        }
    }
}