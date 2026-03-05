package umc.exs.design.strategy.impl;

import org.springframework.stereotype.Component;
import umc.exs.design.strategy.PagamentoStrategy;
import umc.exs.model.dtos.compra.CompraTokensRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PagamentoPixStrategy implements PagamentoStrategy {

    @Override
    public boolean processar(double valor, CompraTokensRequestDTO dados) {
        try {
            // Geramos um ID único para a transação simulada
            String idTransacao = "PX-" + System.currentTimeMillis();
            
            // Payload fake do PIX (Padrão BACEN simplificado para teste)
            String payloadPix = "00020126580014br.gov.bcb.pix0136" + idTransacao;
            
            // URL de um gerador de QR Code gratuito (gera a imagem na hora)
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + payloadPix;

            dados.setPixCopiaECola(payloadPix);
            dados.setQrCodeBase64(qrCodeUrl); 
            dados.setPagamentoId(idTransacao);

            log.info("PIX Simulado gerado para o valor: R$ {}", valor);
            return true; 
        } catch (Exception e) {
            log.error("Erro ao gerar simulação de PIX: {}", e.getMessage());
            return false;
        } 
    }

    @Override
    public String getTipoPagamento() {
        return "PIX";
    }
}