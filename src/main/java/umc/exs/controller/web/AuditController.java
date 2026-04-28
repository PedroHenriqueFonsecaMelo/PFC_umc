package umc.exs.controller.web;

import java.security.Principal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.log.LogAuditoriaService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/historico")
public class AuditController {

    private final ClienteService clienteService;
    private final LogAuditoriaService logAuditoriaService;

    /**
     * Mostra auditoria logs cliente autenticado.
     * Busca ID email, logs data desc, model.
     * Template cliente/auditoria.
     */
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

    /**
     * Retorna logs JSON para cliente autenticado.
     * Mesmo filtro que mostrarAuditoria, AJAX use.
     * Lista ordenada data desc ou vazia se não logado.
     * @param principal usuário
     * @return List<LogAuditoria> logs JSON
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

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller web visualização logs auditoria usuário.
 * /historico/cliente renderiza template com model logs.
 * /cliente/json retorna lista JSON para AJAX.
 * Usa ClienteService e LogAuditoriaService.
 */
