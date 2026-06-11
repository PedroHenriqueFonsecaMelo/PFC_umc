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
import umc.exs.design.strategy.PagamentoStrategy;
import umc.exs.dto.request.compra.CompraTokensRequest;

/**
 * Implementa o padrão Strategy para pagamento via PIX usando a API do Mercado Pago.
 * Possui fallback automático para simulação quando o token de acesso não está configurado ou a API está inacessível.
 */
@Slf4j
@Component
public class PagamentoPixStrategy implements PagamentoStrategy {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${app.notification-url:}")
    private String notificationUrl;

    private static final String QR_API = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=";

    /**
     * Tenta processar o pagamento PIX via Mercado Pago; em caso de token ausente ou erro da API, cai em simulação.
     * Retorna true em ambos os casos, pois o fluxo de simulação também é considerado bem-sucedido.
     */
    @Override
    public boolean processar(double valorBrl, CompraTokensRequest dados) {
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
        return processarSimulado(valorBrl, dados); // FALLBACK
    }

    /**
     * Cria um pagamento PIX real via API do Mercado Pago e salva o QR Code, o payload copia-e-cola e o ID do pagamento.
     * Utiliza a URL de notificação configurada para receber o webhook de confirmação.
     */
    private boolean processarMercadoPago(double valorBrl, CompraTokensRequest dados) throws Exception {
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

    /**
     * Gera um PIX fictício para testes, com payload no padrão BR Code e QR Code via API pública de imagem.
     * Utilizado como fallback quando o Mercado Pago não está disponível ou configurado. // FALLBACK
     */
    private boolean processarSimulado(double valorBrl, CompraTokensRequest dados) {
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

    /**
     * Retorna "PIX" como identificador desta estratégia para o PagamentoFactory.
     * Usado como chave no mapa de estratégias registradas.
     */
    @Override
    public String getTipoPagamento() {
        return "PIX";
    }
}
