package umc.exs.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.log.LogAuditoriaService;
import umc.exs.service.ClienteService;
import umc.exs.model.entidades.foundation.LogAuditoria;

@Controller
@RequestMapping("/historico")
public class AuditController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    @GetMapping("/cliente")
    public String mostrarAuditoria(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/clientes/login";
        }

        String email = principal.getName();

        Long clienteId = clienteService.buscarClientePorEmail(email)
                .map(ClienteDTO::getId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));

        List<LogAuditoria> logs = logAuditoriaService.buscarLogsDoCliente(clienteId);

        model.addAttribute("logs", logs);

        return "cliente/auditoria";
    }

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
