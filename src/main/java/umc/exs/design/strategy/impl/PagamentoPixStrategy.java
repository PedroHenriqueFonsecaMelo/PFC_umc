package umc.exs.design.strategy.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.payment.Payment;

import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.CompraTokensRequestDTO;
import umc.exs.design.strategy.PagamentoStrategy;

@Slf4j
@Component
public class PagamentoPixStrategy implements PagamentoStrategy {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${app.notification-url:}")
    private String notificationUrl;

    private static final String QR_API = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=";

    @Override
    public boolean processar(double valorBrl, CompraTokensRequestDTO dados) {
        if (accessToken != null && !accessToken.isBlank()
                && !accessToken.equals("COLOQUE_SEU_TOKEN_AQUI")) {
            try {
                return processarMercadoPago(valorBrl, dados);
            } catch (MPApiException e) {
                log.warn("MP API erro HTTP {} — {}", e.getStatusCode(),
                        e.getApiResponse() != null ? e.getApiResponse().getContent() : "-");
            } catch (Exception e) {
                log.warn("MP inacessível ({}): {}. Usando simulação.", e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return processarSimulado(valorBrl, dados);
    }

    private boolean processarMercadoPago(double valorBrl, CompraTokensRequestDTO dados) throws Exception {
        MercadoPagoConfig.setAccessToken(accessToken);

        String emailPagador = dados.getEmailPagador();
        log.info("MP PIX — pagador: {} | R$ {}", emailPagador, valorBrl);

        PaymentCreateRequest.PaymentCreateRequestBuilder builder = PaymentCreateRequest.builder()
                .transactionAmount(BigDecimal.valueOf(valorBrl))
                .description("Compra de tokens Bibliotroca")
                .paymentMethodId("pix")
                .payer(PaymentPayerRequest.builder()
                        .email(emailPagador)
                        .build());

        if (notificationUrl != null && !notificationUrl.isBlank()) {
            builder.notificationUrl(notificationUrl);
        }

        PaymentClient client = new PaymentClient();
        Payment payment = client.create(builder.build());

        dados.setPixCopiaECola(payment.getPointOfInteraction().getTransactionData().getQrCode());
        dados.setQrCodeBase64("data:image/png;base64," +
                payment.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        dados.setPagamentoId(payment.getId().toString());

        log.info("PIX MP gerado — ID {} | R$ {}", payment.getId(), valorBrl);
        return true;
    }

    private boolean processarSimulado(double valorBrl, CompraTokensRequestDTO dados) {
        String id = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        String payload = String.format(
            "00020126580014br.gov.bcb.pix0136%s5204000053039865802BR5925Bibliotroca Tokens6009Sao Paulo62070503***6304ABCD",
            id);

        dados.setPixCopiaECola(payload);
        dados.setQrCodeBase64(QR_API + java.net.URLEncoder.encode(payload, java.nio.charset.StandardCharsets.UTF_8));
        dados.setPagamentoId(id);

        log.info("PIX simulado gerado — ID {} | R$ {}", id, valorBrl);
        return true;
    }

    @Override
    public String getTipoPagamento() {
        return "PIX";
    }
}
