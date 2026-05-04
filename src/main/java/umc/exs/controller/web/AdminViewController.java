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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.log.LogAuditoriaService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminViewController {

    private final JwtUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

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
        return "admin/admin_login";
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
            // Tenta carregar o usuário pelo email
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Verifica se é um admin
            if (!userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                model.addAttribute("erro", "Acesso restrito a administradores.");
                return "admin/admin_login";
            }

            // Verifica a senha
            if (!passwordEncoder.matches(senha, userDetails.getPassword())) {
                model.addAttribute("erro", "E-mail ou senha inválidos.");
                return "admin/admin_login";
            }

            // Gera o token JWT
            String token = jwtUtil.generateToken(email);
            jwtUtil.addTokenCookie(response, token);

            // Configura a autenticação no contexto de segurança
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Armazena o token no modelo para ser usado pelo JavaScript
            model.addAttribute("token", token);
            return "redirect:/admin/painel";

        } catch (UsernameNotFoundException e) {
            model.addAttribute("erro", "E-mail ou senha inválidos.");
            return "admin/admin_login";

        }
    }

    /**
     * DESCRIÇÃO DO ARQUIVO:
     * Controller web painel admin (login/logout/painel).
     * Gerencia /admin/login, /painel, /sair com JWT + SecurityContext.
     * Valida ROLE_ADMIN, PasswordEncoder, retorna templates Thymeleaf.
     * Cookie JWT HTTP-only para sessões admin.
     */

    /**
     * Exibe painel admin HTML.
     * Apenas template painel_admin.html.
     * Sem lógica adicional.
     */
    @GetMapping("/painel")
    public String painelAdmin() {

        return "admin/painel_admin";
    }

    /**
     * Exibe dashboard administrativa com métricas e gráficos.
     */
    @GetMapping("/dashboard")
    public String dashboard() {

        return "admin/dashboard";
    }

    /**
     * Logout admin.
     * Clear JWT cookie + SecurityContext.
     * Redirect admin/login?logout.
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

    @GetMapping("/sair")
    public String logout(HttpServletResponse response) {

        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/entrar?logout";
    }
}
