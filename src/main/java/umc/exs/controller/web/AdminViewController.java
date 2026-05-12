package umc.exs.controller.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import umc.exs.dtos.auth.LoginDTO;
import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.log.LogAuditoriaService;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller web painel admin (login/logout/painel).
 * Gerencia /admin/login, /painel, /sair com JWT + SecurityContext.
 * Valida ROLE_ADMIN, PasswordEncoder, retorna templates Thymeleaf.
 * Cookie JWT HTTP-only para sessões admin.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminViewController {

    private final JwtUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;
    private static final String ADMIN_LOGIN = "admin/admin_login";

    /**
     * Exibe página login admin.
     * Prepara LoginDTO model.
     * 
     * @param model LoginDTO
     * @return admin_login.html
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        if (!model.containsAttribute("loginData")) {
            model.addAttribute("loginData", new LoginDTO());
        }
        return ADMIN_LOGIN;
    }

    /**
     * Processa login admin.
     * Valida ROLE_ADMIN + senha, gera JWT cookie.
     * Config SecurityContext, redirect painel.
     * Trata erros 401/404.
     */
    @PostMapping("/login")
    public String processLogin(
            @ModelAttribute("loginData") LoginDTO loginDTO,
            @RequestParam String email,
            @RequestParam String senha,
            Model model,
            HttpServletResponse response) {

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // CORREÇÃO SONARLINT: Substituição de !anyMatch por noneMatch
            if (userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
                model.addAttribute("erro", "Acesso restrito a administradores.");
                return ADMIN_LOGIN;
            }

            if (!passwordEncoder.matches(senha, userDetails.getPassword())) {
                model.addAttribute("erro", "E-mail ou senha inválidos.");
                return ADMIN_LOGIN;
            }

            String token = jwtUtil.generateToken(email);
            jwtUtil.addTokenCookie(response, token);

            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            model.addAttribute("token", token);
            return "redirect:/admin/painel";

        } catch (UsernameNotFoundException e) {
            model.addAttribute("erro", "E-mail ou senha inválidos.");
            return ADMIN_LOGIN;
        }
    }

    /**
     * Exibe painel admin HTML básico.
     * 
     * @return admin/painel_admin.html
     */
    @GetMapping("/painel")
    public String painelAdmin() {
        return "admin/painel_admin";
    }

    /**
     * Exibe dashboard administrativa com métricas e gráficos.
     * 
     * @return admin/dashboard.html
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    /**
     * Exibe página de auditoria com filtros.
     */
    @GetMapping("/audit")
    public String auditoria(
            @RequestParam(required = false) String emailUsuario,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            Model model) {

        List<LogAuditoria> logs = logAuditoriaService.buscarComFiltros(emailUsuario, acao, dataInicio, dataFim);
        List<String> acoes = logAuditoriaService.buscarAcoesDistintas();

        model.addAttribute("logs", logs);
        model.addAttribute("acoes", acoes);
        model.addAttribute("filtroEmail", emailUsuario != null ? emailUsuario : "");
        model.addAttribute("filtroAcao", acao != null ? acao : "");
        model.addAttribute("filtroDataInicio", dataInicio != null ? dataInicio : "");
        model.addAttribute("filtroDataFim", dataFim != null ? dataFim : "");
        model.addAttribute("totalLogs", logs.size());

        return "admin/auditoria";
    }

    /**
     * Exporta logs de auditoria para formato CSV.
     */
    @GetMapping("/audit/exportar-csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarCSV(
            @RequestParam(required = false) String emailUsuario,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {

        List<LogAuditoria> logs = logAuditoriaService.buscarComFiltros(emailUsuario, acao, dataInicio, dataFim);
        String csv = logAuditoriaService.exportarCSV(logs);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        logAuditoriaService.registrarLog("EXPORTACAO_CSV", null, "admin",
                "Exportação CSV de auditoria — " + logs.size() + " registros");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/estoque")
    public String estoque() {
        return "admin/estoque";
    }

    @GetMapping("/cupons")
    public String cupons() {
        return "admin/cupons";
    }

    @GetMapping("/cancelamentos")
    public String cancelamentos() {
        return "admin/cancelamentos";
    }

    @GetMapping("/cancelamentos/{id}")
    public String cancelamentoDetalhe(@PathVariable Long id, Model model) {
        model.addAttribute("solicitacaoId", id);
        return "admin/cancelamento_detalhe";
    }

    @GetMapping("/clientes")
    public String clientes() {
        return "admin/clientes";
    }

    @GetMapping("/clientes/{id}")
    public String clientePerfil(@PathVariable Long id, Model model) {
        model.addAttribute("clienteId", id);
        return "admin/clientes_perfil";
    }

    /**
     * Logout admin: limpa cookie JWT e SecurityContext.
     */
    @GetMapping("/sair")
    public String logout(HttpServletResponse response) {
        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/clientes/login?logout";
    }
}