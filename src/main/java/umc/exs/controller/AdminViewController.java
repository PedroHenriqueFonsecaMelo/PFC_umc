package umc.exs.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.model.dtos.auth.LoginDTO;
import umc.exs.model.daos.repository.AdminRepository;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminRepository adminRepository;

    // Página de login do admin
    @GetMapping("/login")
    public String loginPage(Model model) {
        if (!model.containsAttribute("loginData")) {
            model.addAttribute("loginData", new LoginDTO());
        }
        return "admin/admin_login";
    }

    // Processa login do admin
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
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Armazena o token no modelo para ser usado pelo JavaScript
            model.addAttribute("token", token);
            return "redirect:/admin/painel";

        } catch (UsernameNotFoundException e) {
            model.addAttribute("erro", "E-mail ou senha inválidos.");
            return "admin/admin_login";
        }
    }

    // Página do painel admin
    @GetMapping("/painel")
    public String painelAdmin() {
        return "admin/painel_admin";
    }

    // Logout para admin
    @GetMapping("/sair")
    public String logout(HttpServletResponse response) {
        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/admin/login?logout";
    }
}
