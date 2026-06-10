package umc.exs.service.carteira.delegado;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarteiraEmailService {

    private static final String ASSUNTO_EMAIL = "Atualização de saldo — Bibliotroca";

    private final EmailFacade emailFacade;
    private final LogAuditoriaService auditoria;

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

            auditoria.registrarLog(
                    AcaoAuditoria.CARTEIRA_TOKEN_ADICIONADO.name(),
                    String.format("E-mail de crédito enviado | Valor: %.2f | Método: %s",
                            valor, metodo));

            log.info("Email crédito enviado clienteId={} valor={} metodo={}",
                    cliente.getId(), valor, metodo);

        } catch (Exception e) {

            log.error("Erro ao enviar e-mail de crédito clienteId={} erro={}",
                    cliente.getId(), e.getMessage());

            auditoria.registrarLog(
                    AcaoAuditoria.GENERICO.name(),
                    "Falha ao enviar e-mail de crédito: " + e.getMessage());
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

            auditoria.registrarLog(
                    AcaoAuditoria.CARTEIRA_TOKEN_DEBITADO.name(),
                    String.format("E-mail de débito enviado | Valor: %.2f | Descrição: %s",
                            valor, descricao));

            log.info("Email débito enviado clienteId={} valor={}",
                    cliente.getId(), valor);

        } catch (Exception e) {

            log.error("Erro ao enviar e-mail de débito clienteId={} erro={}",
                    cliente.getId(), e.getMessage());

            auditoria.registrarLog(
                    AcaoAuditoria.GENERICO.name(),
                    "Falha ao enviar e-mail de débito: " + e.getMessage());
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

            auditoria.registrarLog(
                    AcaoAuditoria.PAGAMENTO_PIX_CONFIRMADO.name(),
                    String.format("E-mail PIX enviado | Valor: %.2f", valorPix));

            log.info("Email PIX enviado clienteId={} valor={}",
                    cliente.getId(), valorPix);

        } catch (Exception e) {

            log.error("Erro ao enviar e-mail PIX clienteId={} erro={}",
                    cliente.getId(), e.getMessage());

            auditoria.registrarLog(
                    AcaoAuditoria.GENERICO.name(),
                    "Falha ao enviar e-mail PIX: " + e.getMessage());
        }
    }
}