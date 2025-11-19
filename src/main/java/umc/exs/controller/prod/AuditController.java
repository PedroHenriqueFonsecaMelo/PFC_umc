package umc.exs.controller.prod;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import umc.exs.backstage.log.LogAuditoriaService;
import umc.exs.backstage.service.ClienteService;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.entidades.foundation.LogAuditoria;

@Controller
@RequestMapping("/historico")
public class AuditController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    /**
     * Exibe a página com os logs do cliente
     */
    @GetMapping("/cliente")
    public String mostrarAuditoria(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/clientes/login";
        }

        System.out.println("principal: " + principal);

        String email = principal.getName();

        Long clienteId = clienteService.buscarClientePorEmail(email)
                .map(ClienteDTO::getId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));
                
        System.out.println("DEBUG: clienteId = " + clienteId);
        List<LogAuditoria> logs = logAuditoriaService.buscarLogsDoCliente(clienteId);

        model.addAttribute("logs", logs);

        return "cliente/auditoria"; // sua página thymeleaf
    }

    /**
     * Retorna logs em formato JSON (útil para AJAX)
     */
    @GetMapping("/cliente/json")
    @ResponseBody
    public List<LogAuditoria> listarLogsJson(Principal principal) {

        if (principal == null) {
            return List.of();
        }

        String email = principal.getName();

        Long clienteId = clienteService.buscarClientePorEmail(email)
                .map(ClienteDTO::getId)
                .orElse(0L);

        return logAuditoriaService.buscarLogsDoCliente(clienteId);
    }

}
