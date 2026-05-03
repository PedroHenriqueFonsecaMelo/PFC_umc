package umc.exs.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import umc.exs.DTOs.admin.EmailDestinatarioDTO;
import umc.exs.DTOs.admin.EmailDisparoDTO;
import umc.exs.service.core.control.NotificacaoEmailService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notificacoes")
public class NotificacaoViewController {

    private final NotificacaoEmailService notificacaoEmailService;

    @GetMapping
    public String pagina(Model model) {
        model.addAttribute("emailDTO", new EmailDisparoDTO());
        return "admin/notificacoes";
    }

    @GetMapping("/preview")
    @ResponseBody
    public ResponseEntity<List<EmailDestinatarioDTO>> preview(
            @RequestParam(defaultValue = "todos") String filtro,
            @RequestParam(defaultValue = "0") Integer limite) {
        return ResponseEntity.ok(notificacaoEmailService.filtrarDestinatarios(filtro, limite));
    }

    @PostMapping("/disparar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> disparar(@RequestBody EmailDisparoDTO dto) {
        String mensagem = notificacaoEmailService.dispararOuAgendar(dto);
        return ResponseEntity.ok(Map.of("mensagem", mensagem));
    }
}
