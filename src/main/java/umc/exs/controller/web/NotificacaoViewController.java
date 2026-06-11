package umc.exs.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import umc.exs.dto.request.admin.EmailDisparoRequest;
import umc.exs.dto.response.email.EmailDestinatarioResponse;
import umc.exs.dto.response.email.EmailHistoricoResponse;
import umc.exs.service.email.notificacao.NotificacaoEmailService;

import java.util.List;
import java.util.Map;

/**
 * Gerencia o painel de disparo de e-mails segmentados do admin: preview de destinatários, envio e histórico.
 * Permite filtrar os destinatários por segmento (todos, por XP ou por saldo) antes de disparar a campanha.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notificacoes")
public class NotificacaoViewController {

    private final NotificacaoEmailService notificacaoEmailService;

    /**
     * Exibe a página de notificações do admin com o formulário de disparo de e-mail.
     * Inicializa o model com um EmailDisparoRequest vazio para o formulário Thymeleaf.
     */
    @GetMapping
    public String pagina(Model model) {
        model.addAttribute("emailDTO", new EmailDisparoRequest());
        return "admin/notificacoes";
    }

    /**
     * Retorna a lista de destinatários filtrados por segmento (todos, por XP ou por saldo) com limite opcional.
     * Utilizado pelo admin para visualizar quem receberá o e-mail antes de disparar.
     */
    @GetMapping("/preview")
    @ResponseBody
    public ResponseEntity<List<EmailDestinatarioResponse>> preview(
            @RequestParam(defaultValue = "todos") String filtro,
            @RequestParam(defaultValue = "0") Integer limite) {
        return ResponseEntity.ok(notificacaoEmailService.filtrarDestinatarios(filtro, limite));
    }

    /**
     * Envia ou agenda o e-mail para os destinatários filtrados conforme os dados do EmailDisparoRequest.
     * Retorna uma mensagem de confirmação indicando se o disparo foi imediato ou agendado.
     */
    @PostMapping("/disparar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> disparar(@RequestBody @Valid EmailDisparoRequest dto) {
        String mensagem = notificacaoEmailService.dispararOuAgendar(dto);
        return ResponseEntity.ok(Map.of("mensagem", mensagem));
    }

    /**
     * Retorna o histórico de disparos de e-mail realizados pelo admin em formato JSON.
     * Utilizado para auditoria e acompanhamento das campanhas enviadas.
     */
    @GetMapping("/historico")
    @ResponseBody
    public ResponseEntity<List<EmailHistoricoResponse>> historico() {
        return ResponseEntity.ok(notificacaoEmailService.listarHistorico());
    }
}
