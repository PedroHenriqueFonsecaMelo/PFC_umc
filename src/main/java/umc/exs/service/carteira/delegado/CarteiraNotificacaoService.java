package umc.exs.service.carteira.delegado;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.notificacao.NotificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarteiraNotificacaoService {

        private final NotificacaoService notificacaoService;

        public void notificarRecarga(
                        Cliente cliente,
                        double valor,
                        String metodo) {

                notificacaoService.notificarSaldo(
                                cliente.getId(),
                                cliente.getSaldoTokens(),
                                String.format(
                                                "Recarga de T$ %.2f via %s",
                                                valor,
                                                metodo));

                try {

                        notificacaoService.criarNotificacaoDashboard(
                                        cliente,
                                        String.format(
                                                        "Recarga confirmada! T$ %.2f adicionados ao seu saldo.",
                                                        valor),
                                        "/clientes/carteira");

                } catch (Exception e) {
                        log.error("Erro ao criar notificação de recarga: {}", e.getMessage());
                }
        }

        public void notificarDebito(
                        Cliente cliente,
                        double valor,
                        String descricao) {

                notificacaoService.notificarSaldo(
                                cliente.getId(),
                                cliente.getSaldoTokens(),
                                String.format(
                                                "Débito de T$ %.2f: %s",
                                                valor,
                                                descricao));
        }

        public void notificarPixConfirmado(
                        Cliente cliente,
                        double valorPix) {

                notificacaoService.notificarSaldo(
                                cliente.getId(),
                                cliente.getSaldoTokens(),
                                String.format(
                                                "Recarga PIX de T$ %.2f confirmada",
                                                valorPix));

                try {

                        notificacaoService.criarNotificacaoDashboard(
                                        cliente,
                                        String.format(
                                                        "Recarga confirmada! T$ %.2f adicionados ao seu saldo via PIX.",
                                                        valorPix),
                                        "/clientes/carteira");

                } catch (Exception e) {
                        log.error("Erro ao criar notificação PIX: {}", e.getMessage());
                }
        }
}