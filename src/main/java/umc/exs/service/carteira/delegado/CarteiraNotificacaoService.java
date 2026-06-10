package umc.exs.service.carteira.delegado;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarteiraNotificacaoService {

    private final NotificacaoService notificacaoService;
    private final LogAuditoriaService auditoria;

    public void notificarRecarga(
            Cliente cliente,
            double valor,
            String metodo) {

        notificacaoService.notificarSaldo(
                cliente.getId(),
                cliente.getSaldoTokens(),
                String.format("Recarga de T$ %.2f via %s", valor, metodo));

        auditoria.registrarLog(
                AcaoAuditoria.CARTEIRA_TOKEN_ADICIONADO.name(),
                String.format("Notificação de recarga enviada | Valor: %.2f | Método: %s",
                        valor, metodo));

        try {

            notificacaoService.criarNotificacaoDashboard(
                    cliente,
                    String.format("Recarga confirmada! T$ %.2f adicionados ao seu saldo.", valor),
                    "/clientes/carteira");

            log.info("Notificação dashboard recarga clienteId={} valor={}",
                    cliente.getId(), valor);

        } catch (Exception e) {

            log.error("Erro ao criar notificação de recarga clienteId={} erro={}",
                    cliente.getId(), e.getMessage());

            auditoria.registrarLog(
                    AcaoAuditoria.GENERICO.name(),
                    "Falha ao criar notificação de recarga: " + e.getMessage());
        }
    }

    public void notificarDebito(
            Cliente cliente,
            double valor,
            String descricao) {

        notificacaoService.notificarSaldo(
                cliente.getId(),
                cliente.getSaldoTokens(),
                String.format("Débito de T$ %.2f: %s", valor, descricao));

        auditoria.registrarLog(
                AcaoAuditoria.CARTEIRA_TOKEN_DEBITADO.name(),
                String.format("Notificação de débito enviada | Valor: %.2f | Descrição: %s",
                        valor, descricao));

        log.info("Notificação débito enviada clienteId={} valor={}",
                cliente.getId(), valor);
    }

    public void notificarPixConfirmado(
            Cliente cliente,
            double valorPix) {

        notificacaoService.notificarSaldo(
                cliente.getId(),
                cliente.getSaldoTokens(),
                String.format("Recarga PIX de T$ %.2f confirmada", valorPix));

        auditoria.registrarLog(
                AcaoAuditoria.PAGAMENTO_PIX_CONFIRMADO.name(),
                String.format("Notificação PIX enviada | Valor: %.2f", valorPix));

        try {

            notificacaoService.criarNotificacaoDashboard(
                    cliente,
                    String.format("Recarga confirmada! T$ %.2f adicionados ao seu saldo via PIX.", valorPix),
                    "/clientes/carteira");

            log.info("Notificação dashboard PIX clienteId={} valor={}",
                    cliente.getId(), valorPix);

        } catch (Exception e) {

            log.error("Erro ao criar notificação PIX clienteId={} erro={}",
                    cliente.getId(), e.getMessage());

            auditoria.registrarLog(
                    AcaoAuditoria.GENERICO.name(),
                    "Falha ao criar notificação PIX: " + e.getMessage());
        }
    }
}