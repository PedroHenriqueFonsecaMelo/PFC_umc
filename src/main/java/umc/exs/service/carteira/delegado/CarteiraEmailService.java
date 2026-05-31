package umc.exs.service.carteira.delegado;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarteiraEmailService {

    private static final String ASSUNTO_EMAIL = "Atualização de saldo — Bibliotroca";

    private final EmailFacade emailFacade;

    public void enviarCredito(
            Cliente cliente,
            double saldoAnterior,
            double valor,
            String metodo) {

        try {

            String motivo = switch (metodo.toUpperCase()) {
                case "PIX" -> "Recarga via PIX";
                case "CUPOM" -> "Resgate de cupom";
                default -> "Crédito — " + metodo;
            };

            emailFacade.sendHtmlSafe(
                    cliente.getEmail(),
                    ASSUNTO_EMAIL,
                    EmailHtmlBuilder.atualizacaoSaldo(
                            cliente.getNome(),
                            saldoAnterior,
                            valor,
                            cliente.getSaldoTokens(),
                            motivo,
                            true,
                            LocalDateTime.now()));

        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de crédito: {}", e.getMessage());
        }
    }

    public void enviarDebito(
            Cliente cliente,
            double saldoAnterior,
            double valor,
            String descricao) {

        try {

            emailFacade.sendHtmlSafe(
                    cliente.getEmail(),
                    ASSUNTO_EMAIL,
                    EmailHtmlBuilder.atualizacaoSaldo(
                            cliente.getNome(),
                            saldoAnterior,
                            valor,
                            cliente.getSaldoTokens(),
                            descricao,
                            false,
                            LocalDateTime.now()));

        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de débito: {}", e.getMessage());
        }
    }

    public void enviarConfirmacaoPix(
            Cliente cliente,
            double saldoAnterior,
            double valorPix) {

        try {

            emailFacade.sendHtmlSafe(
                    cliente.getEmail(),
                    ASSUNTO_EMAIL,
                    EmailHtmlBuilder.atualizacaoSaldo(
                            cliente.getNome(),
                            saldoAnterior,
                            valorPix,
                            cliente.getSaldoTokens(),
                            "Recarga via PIX confirmada",
                            true,
                            LocalDateTime.now()));

        } catch (Exception e) {
            log.error("Erro ao enviar e-mail PIX: {}", e.getMessage());
        }
    }
}