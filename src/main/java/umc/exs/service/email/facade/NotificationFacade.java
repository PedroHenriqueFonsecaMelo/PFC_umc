package umc.exs.service.email.facade;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.notificacao.NotificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificacaoService notificacaoService;

    public void notifySafe(@Nonnull Cliente cliente, String msg, String link) {

        try {
            notificacaoService.criarNotificacaoDashboard(cliente, msg, link);
        } catch (Exception e) {
            log.error("Erro ao notificar {}", cliente.getEmail(), e);
        }
    }
}
