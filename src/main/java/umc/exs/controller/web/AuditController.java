package umc.exs.controller.web;

import java.security.Principal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.log.LogAuditoriaService;

/**
 * Exibe o histórico de auditoria do cliente autenticado via página Thymeleaf e endpoint JSON para AJAX.
 * Mapeia as rotas /historico/cliente (página) e /historico/cliente/json (resposta JSON).
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/historico")
public class AuditController {

    private final ClienteService clienteService;
    private final LogAuditoriaService logAuditoriaService;

    /**
     * Exibe a página de auditoria com os logs de ações do cliente autenticado,
     * ordenados do mais recente ao mais antigo.
     */
    @GetMapping("/cliente")

    public String mostrarAuditoria(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/clientes/login";
        }

        String email = principal.getName();

        Long clienteId = clienteService.buscarClientePorEmail(email)
                .map(Cliente::getId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));

        List<LogAuditoria> logs = logAuditoriaService.buscarLogsDoCliente(clienteId);

        model.addAttribute("logs", logs);

        return "cliente/auditoria";
    }

    /**
     * Retorna os logs de auditoria do cliente autenticado em formato JSON para
     * consumo via AJAX. Retorna lista vazia se o usuário não estiver autenticado.
     */
    @GetMapping("/cliente/json")
    @ResponseBody
    public List<LogAuditoria> listarLogsJson(Principal principal) {

        if (principal == null) {
            return List.of();
        }

        String email = principal.getName();

        Long clienteId = clienteService.buscarClientePorEmail(email)
                .map(Cliente::getId)
                .orElse(0L);

        return logAuditoriaService.buscarLogsDoCliente(clienteId);
    }
}
