package umc.exs.controller.api.control;

import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.model.entidades.foundation.EmailVerificacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.foundation.EmailVerificacaoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final ClienteService clienteService;
    private final AuthHelper authHelper;
    private final EmailVerificacaoRepository emailVerificacaoRepository;
    private final ClienteRepository clienteRepository;

    // ── LOGIN ────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDto,
            HttpServletResponse response,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            ClienteDTO cliente = clienteService.autenticarCliente(loginDto.getEmail(), loginDto.getSenha());
            String token = jwtUtil.generateToken(cliente.getEmail());
            authHelper.addTokenCookie(response, token);

            // Registra sessão ativa (SHA-256 do token)
            Cliente entidade = clienteRepository.findById(cliente.getId()).orElse(null);
            if (entidade != null) {
                String ip = request.getRemoteAddr();
                String ua = request.getHeader("User-Agent");
            }

            log.info("Login API: {}", cliente.getEmail());
            return ResponseEntity.ok(Map.of("message", "Login bem-sucedido", "token", token));
        } catch (IllegalArgumentException e) {
            // Mensagem genérica na rota pública — evita enumeração de e-mails
            return ResponseEntity.status(401).body(Map.of("error", "E-mail ou senha inválidos."));
        }
    }

    // ── LOGOUT ───────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request,
            HttpServletResponse response) {
        String token = resolveToken(request);
        if (token != null) {
        }
        // Remove cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("mensagem", "Logout realizado com sucesso."));
    }

    private String resolveToken(jakarta.servlet.http.HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if ("token".equalsIgnoreCase(c.getName())) return c.getValue();
            }
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }

    // ── VERIFICAR E-MAIL ─────────────────────────────────────────────
    @GetMapping("/verificar-email")
    public ResponseEntity<?> verificarEmail(@RequestParam String token) {
        EmailVerificacao verificacao = emailVerificacaoRepository.findByToken(token).orElse(null);

        if (verificacao == null || verificacao.isUsado()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Token inválido ou já utilizado."));
        }
        if (verificacao.isExpirado()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Token expirado. Solicite um novo link."));
        }

        Cliente cliente = verificacao.getCliente();
        cliente.setEmailVerificado(true);
        clienteRepository.save(cliente);

        verificacao.setUsado(true);
        emailVerificacaoRepository.save(verificacao);

        log.info("E-mail verificado com sucesso para: {}", cliente.getEmail());
        return ResponseEntity.status(302)
                .header("Location", "/clientes/login?emailVerificado=ok")
                .build();
    }

    // ── DEV: verificar e-mail diretamente (apenas profile local) ─────
    @Profile("local")
    @GetMapping("/dev/verificar-email/{clienteId}")
    public ResponseEntity<?> devVerificarEmail(@PathVariable Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            return ResponseEntity.notFound().build();
        }
        cliente.setEmailVerificado(true);
        clienteRepository.save(cliente);
        log.info("[DEV] E-mail do cliente ID {} marcado como verificado.", clienteId);
        return ResponseEntity.ok(Map.of(
                "mensagem", "E-mail verificado com sucesso (modo desenvolvimento).",
                "clienteId", clienteId,
                "email", cliente.getEmail()));
    }

    // ── REGISTER ─────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody SignupDTO signupDTO, HttpServletResponse response) {

        ClienteDTO clienteSalvo = clienteService.salvarCliente(signupDTO);

        String token = jwtUtil.generateToken(clienteSalvo.getEmail());
        authHelper.addTokenCookie(response, token);

        log.info("Novo cliente API: {}", clienteSalvo.getEmail());
        return ResponseEntity.status(201).body(Map.of(
                "message", "Cliente registrado com sucesso",
                "token", token,
                "cliente", clienteSalvo));
    }
}