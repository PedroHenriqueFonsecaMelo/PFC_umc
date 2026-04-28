package umc.exs.controller.web;

import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final JwtUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final ClienteService clienteService;
    private final AuthHelper authHelper;

    @GetMapping({ "/", "/index", "/home" })
    public String index() {
        return "index";
    }

    @GetMapping("/entrar")
    public String entrar() {
        return "entrar";
    }

    /**
     * Login unificado: verifica o e-mail e redireciona para admin ou cliente.
     */
    @PostMapping("/entrar")
    public String processarLogin(
            @RequestParam String email,
            @RequestParam String senha,
            Model model,
            HttpServletResponse response) {

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN"));

            if (isAdmin) {
                if (!passwordEncoder.matches(senha, userDetails.getPassword())) {
                    model.addAttribute("erro", "E-mail ou senha inválidos.");
                    model.addAttribute("emailAnterior", email);
                    return "entrar";
                }
                String token = jwtUtil.generateToken(email);
                jwtUtil.addTokenCookie(response, token);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                return "redirect:/admin/painel";
            }

        } catch (UsernameNotFoundException ignored) {
            // usuário não encontrado — trata como credenciais inválidas abaixo
        }

        Optional<ClienteDTO> clienteOpt = clienteService.autenticarCliente(email, senha);
        if (clienteOpt.isEmpty()) {
            model.addAttribute("erro", "E-mail ou senha inválidos.");
            model.addAttribute("emailAnterior", email);
            return "entrar";
        }

        ClienteDTO cliente = clienteOpt.get();
        authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");
        return "redirect:/";
    }

    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller rotas raiz site (home/shop/login/admin).
 * Login unificado em /entrar: detecta admin ou cliente pelo e-mail.
 */
