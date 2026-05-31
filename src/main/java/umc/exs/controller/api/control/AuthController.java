package umc.exs.controller.api.control;

import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import umc.exs.dto.request.cliente.LoginRequest;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.model.entidades.foundation.EmailVerificacao;
import umc.exs.model.entidades.usuario.Cliente;

import umc.exs.repository.foundation.EmailVerificacaoRepository;
import umc.exs.repository.usuario.ClienteRepository;

import umc.exs.security.JwtUtil;
import umc.exs.service.cliente.ClienteService;
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

    private static final String COOKIE_TOKEN = "token";

    // ───────────────────────── LOGIN ─────────────────────────

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest LoginRequest,
            HttpServletResponse response,
            HttpServletRequest request) {

        try {
            Cliente cliente = clienteService.autenticarCliente(
                    LoginRequest.getEmail(),
                    LoginRequest.getSenha());

            String token = jwtUtil.generateToken(cliente.getEmail());
            authHelper.addTokenCookie(response, token);

            String ip = request.getRemoteAddr();
            String ua = request.getHeader("User-Agent");

            log.info("Login realizado: email={}, ip={}, userAgent={}",
                    cliente.getEmail(), ip, ua);

            return ResponseEntity.ok(Map.of(
                    "message", "Login bem-sucedido",
                    COOKIE_TOKEN, token));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "E-mail ou senha inválidos."));
        }
    }

    // ───────────────────────── LOGOUT ─────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = resolveToken(request);

        if (token != null) {
            log.info("Logout executado");
        }

        Cookie cookie = new Cookie(COOKIE_TOKEN, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);

        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
                "mensagem", "Logout realizado com sucesso."));
    }

    // ───────────────────────── HELPERS ─────────────────────────

    private String resolveToken(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_TOKEN.equalsIgnoreCase(c.getName())) {
                    return c.getValue();
                }
            }
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

    // ───────────────────────── VERIFICAR EMAIL ─────────────────────────

    @GetMapping("/verificar-email")
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam String token) {

        EmailVerificacao verificacao = emailVerificacaoRepository.findByToken(token).orElse(null);

        if (verificacao == null || verificacao.isUsado()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Token inválido ou já utilizado."));
        }

        if (verificacao.isExpirado()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Token expirado. Solicite novo link."));
        }

        Cliente cliente = verificacao.getCliente();
        cliente.setEmailVerificado(true);
        clienteRepository.save(cliente);

        verificacao.setUsado(true);
        emailVerificacaoRepository.save(verificacao);

        log.info("Email verificado: {}", cliente.getEmail());

        return ResponseEntity.status(302)
                .header("Location", "/clientes/login?emailVerificado=ok")
                .build();
    }

    // ───────────────────────── DEV ONLY ─────────────────────────

    @Profile("local")
    @GetMapping("/dev/verificar-email/{clienteId}")
    public ResponseEntity<Map<String, Object>> devVerificarEmail(@PathVariable @NonNull Long clienteId) {

        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);

        if (cliente == null) {
            return ResponseEntity.notFound().build();
        }

        cliente.setEmailVerificado(true);
        clienteRepository.save(cliente);

        log.info("[DEV] Email verificado cliente ID={}", clienteId);

        return ResponseEntity.ok(Map.of(
                "mensagem", "Email verificado (modo dev)",
                "clienteId", clienteId,
                "email", cliente.getEmail()));
    }

    // ───────────────────────── REGISTER ─────────────────────────

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody SignupRequest SignupRequest,
            HttpServletResponse response) {

        Cliente clienteSalvo = clienteService.salvarCliente(SignupRequest);

        String token = jwtUtil.generateToken(clienteSalvo.getEmail());
        authHelper.addTokenCookie(response, token);

        log.info("Novo cliente registrado: {}", clienteSalvo.getEmail());

        return ResponseEntity.status(201).body(Map.of(
                "message", "Cliente registrado com sucesso",
                COOKIE_TOKEN, token,
                "cliente", clienteSalvo));
    }
}