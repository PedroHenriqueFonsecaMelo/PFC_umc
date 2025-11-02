package umc.exs.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import umc.exs.backstage.security.JwtUserDetailsService;
import umc.exs.backstage.security.JwtUtil;
import umc.exs.backstage.service.ClienteService;
import umc.exs.backstage.service.FieldValidation;
import umc.exs.model.DTO.auth.SignupDTO;
import umc.exs.model.DTO.user.ClienteDTO;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;

@Controller
@RequestMapping("/clientes")
public class ClientController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteService clientService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    // ==============================
    // 🔹 Utilitários internos
    // ==============================

    private void addJwtCookie(HttpServletResponse response, String jwt) {
        if (jwt == null) return;
        Cookie cookie = new Cookie("token", jwt);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 3600);
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", "token=" + jwt + "; Path=/; Max-Age=" + (7 * 24 * 3600) + "; HttpOnly; SameSite=Lax");
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", "token=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    }

    private boolean isAdminByEmail(String email) {
        if (email == null) return false;
        try {
            UserDetails ud = jwtUserDetailsService.loadUserByUsername(email);
            return ud.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        } catch (UsernameNotFoundException e) {
            return false;
        }
    }

    // ==============================
    // 🔹 Cadastro
    // ==============================

    @GetMapping("/cadastro")
    public String cadastroCliente(Model model) {
        if (!model.containsAttribute("cliente")) model.addAttribute("cliente", new ClienteDTO());
        return "cliente/cadastro_cliente";
    }

    @PostMapping("/cadastro")
    public String criarCliente(@ModelAttribute SignupDTO signupDTO,
                               @RequestParam(value = "termsAccepted", required = false) Boolean termsAccepted,
                               @RequestParam(value = "privacyAccepted", required = false) Boolean privacyAccepted,
                               Model model,
                               HttpServletResponse response) {

        if (termsAccepted == null || !termsAccepted || privacyAccepted == null || !privacyAccepted) {
            model.addAttribute("erro", "É necessário aceitar os termos e a política de privacidade!");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        if (signupDTO == null || !FieldValidation.validarCampos(signupDTO) || !FieldValidation.isValidEmail(signupDTO.getEmail())) {
            model.addAttribute("erro", "Dados inválidos.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        String email = FieldValidation.sanitizeEmail(signupDTO.getEmail());
        if (clienteRepository.findByEmail(email).isPresent()) {
            model.addAttribute("erro", "Email já cadastrado.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        signupDTO.setEmail(email);
        signupDTO.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

        ClienteDTO saved = clientService.salvarCliente(signupDTO);
        if (saved == null || saved.getId() == null) {
            model.addAttribute("erro", "Erro ao cadastrar cliente.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        try {
            String jwt = jwtUtil.generateToken(saved.getEmail());
            addJwtCookie(response, jwt);

            UserDetails ud = jwtUserDetailsService.loadUserByUsername(saved.getEmail());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ignored) {}

        model.addAttribute("cliente", saved);
        return "cliente/homepage";
    }

    // ==============================
    // 🔹 Login
    // ==============================

    @GetMapping("/login")
    public String loginForm() {
        return "cliente/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String senha,
                        Model model,
                        HttpServletResponse response) {
        try {
            String ema = FieldValidation.sanitizeEmail(email);
            if (ema == null || !FieldValidation.isValidEmail(ema)) {
                model.addAttribute("erro", "Email inválido");
                return "cliente/login";
            }

            UserDetails ud = jwtUserDetailsService.loadUserByUsername(ema);
            if (ud == null || !passwordEncoder.matches(senha, ud.getPassword())) {
                model.addAttribute("erro", "Credenciais inválidas");
                return "cliente/login";
            }

            String jwt = jwtUtil.generateToken(ema);
            addJwtCookie(response, jwt);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<Cliente> opt = clienteRepository.findByEmail(ema);
            if (opt.isPresent()) {
                Optional<ClienteDTO> dto = clientService.buscarClientePorId(opt.get().getId());
                model.addAttribute("cliente", dto.orElse(null));
                return "cliente/homepage";
            } else {
                return "cliente/cliente_nao_encontrado";
            }
        } catch (Exception e) {
            model.addAttribute("erro", "Erro no login");
            return "cliente/login";
        }
    }

    // ==============================
    // 🔹 Logout
    // ==============================

    @GetMapping("/logout")
    public String logout(HttpServletResponse response, Model model) {
        clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        model.addAttribute("mensagem", "Você saiu com sucesso.");
        return "cliente/login";
    }

    // ==============================
    // 🔹 Homepage autenticada
    // ==============================

    @GetMapping("/homepage")
    public String homepage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "cliente/acesso_negado";

        String email = auth.getName();
        Optional<Cliente> opt = clienteRepository.findByEmail(email);
        if (opt.isEmpty()) return "cliente/acesso_negado";

        Optional<ClienteDTO> dto = clientService.buscarClientePorId(opt.get().getId());
        if (dto.isPresent()) {
            model.addAttribute("cliente", dto.get());
            return "cliente/homepage";
        } else {
            return "cliente/cliente_nao_encontrado";
        }
    }

    // ==============================
    // 🔹 Detalhes / Remoção (admin ou dono)
    // ==============================

    @GetMapping("/{id}")
    public String buscarCliente(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "cliente/acesso_negado";

        String email = auth.getName();
        Optional<Cliente> authCliente = clienteRepository.findByEmail(email);
        if (authCliente.isEmpty()) return "cliente/acesso_negado";

        boolean isAdmin = isAdminByEmail(email);
        if (!isAdmin && !authCliente.get().getId().equals(id)) return "cliente/acesso_negado";

        Optional<ClienteDTO> clienteOpt = clientService.buscarClientePorId(id);
        if (clienteOpt.isPresent()) {
            model.addAttribute("cliente", clienteOpt.get());
            return "cliente/detalhes_cliente";
        } else {
            return "cliente/cliente_nao_encontrado";
        }
    }

    @GetMapping("/{id}/remover")
    public String exibirRemocaoCliente(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "cliente/acesso_negado";

        String email = auth.getName();
        Optional<Cliente> authCliente = clienteRepository.findByEmail(email);
        if (authCliente.isEmpty()) return "cliente/acesso_negado";

        boolean isAdmin = isAdminByEmail(email);
        if (!isAdmin && !authCliente.get().getId().equals(id)) return "cliente/acesso_negado";

        Optional<ClienteDTO> clienteOpt = clientService.buscarClientePorId(id);
        if (clienteOpt.isPresent()) {
            model.addAttribute("cliente", clienteOpt.get());
            return "cliente/remover_cliente";
        } else {
            return "cliente/cliente_nao_encontrado";
        }
    }

    @PostMapping("/{id}/remover")
    public String removerCliente(@PathVariable Long id, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "cliente/acesso_negado";

        String email = auth.getName();
        Optional<Cliente> authCliente = clienteRepository.findByEmail(email);
        if (authCliente.isEmpty()) return "cliente/acesso_negado";

        boolean isAdmin = isAdminByEmail(email);
        if (!isAdmin && !authCliente.get().getId().equals(id)) return "cliente/acesso_negado";

        clienteRepository.deleteById(id);

        if (authCliente.get().getId().equals(id)) {
            clearJwtCookie(response);
            SecurityContextHolder.clearContext();
        }

        return "cliente/cliente_removido";
    }
}
