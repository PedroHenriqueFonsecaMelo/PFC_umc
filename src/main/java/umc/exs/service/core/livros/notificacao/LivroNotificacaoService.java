package umc.exs.service.core.livros.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.core.dashboard.ListaDesejosService;
import umc.exs.service.notificacao.NotificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroNotificacaoService {

    private final ListaDesejosService listaDesejosService;
    private final NotificacaoService notificacaoService;

    public void notificarWishlistSeDisponivel(String isbn, String titulo) {
        // mantém comportamento atual: falhas no side-effect não devem quebrar fluxo
        try {
            listaDesejosService.notificarClientesSeDisponivel(isbn, titulo);
        } catch (Exception e) {
            log.error("Erro wishlist: {}", e.getMessage());
        }
    }

    public void notificarAprovacaoDashboard(Cliente vendedor, String titulo, double tokenReward) {
        try {
            notificacaoService.criarNotificacaoDashboard(
                    vendedor,
                    String.format(
                            "Seu livro '%s' foi aprovado e está na vitrine! Você recebeu T$ %.2f de bônus.",
                            titulo,
                            tokenReward),
                    "/livros/vitrine");
        } catch (Exception e) {
            log.error("Erro ao notificar aprovação para vendedor {}: {}",
                    vendedor.getEmail(),
                    e.getMessage());
        }
    }

    public void notificarRejeicaoDashboard(Cliente vendedor, String titulo, String comentario) {
        try {
            String motivoTexto = (comentario != null && !comentario.isBlank())
                    ? " Motivo: " + comentario
                    : "";
            notificacaoService.criarNotificacaoDashboard(
                    vendedor,
                    String.format("Seu livro '%s' foi rejeitado.%s", titulo, motivoTexto),
                    "/clientes/homepage");
        } catch (Exception e) {
            log.error("Erro ao notificar rejeição para vendedor {}: {}",
                    vendedor.getEmail(),
                    e.getMessage());
        }
    }
}
