package umc.exs.controller.api.control;

import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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

/**
 * Controller REST responsável pela autenticação via API (login, logout, registro e verificação de e-mail).
 * Gera e invalida tokens JWT armazenados em cookie HTTP-only.
 */
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

    /**
     * Realiza o login via API REST, autentica o cliente e retorna o token JWT em cookie.
     * Registra IP e User-Agent para fins de auditoria.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("|")) {
                String[] parts = msg.split("\\|", 2);
                String texto = parts[1];
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "/clientes/login?erro=" + java.net.URLEncoder.encode(texto, java.nio.charset.StandardCharsets.UTF_8))
                        .build();
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/clientes/login?erro=" + java.net.URLEncoder.encode("E-mail ou senha inválidos.", java.nio.charset.StandardCharsets.UTF_8))
                    .build();
        }
    }

    // ───────────────────────── LOGOUT ─────────────────────────

    /** Realiza o logout invalidando o cookie JWT e limpando a sessão do cliente. */
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

    /**
     * Extrai o token JWT da requisição verificando primeiro o cookie e depois o header Authorization.
     * Retorna null se nenhum token for encontrado.
     */
    private String resolveToken(HttpServletRequest request) {

        // Tenta extrair o token do cookie HTTP-only definido no login
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_TOKEN.equalsIgnoreCase(c.getName())) {
                    return c.getValue();
                }
            }
        }

        // Fallback — aceita token no header Authorization (Bearer) para clientes de API
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

    // ───────────────────────── VERIFICAR EMAIL ─────────────────────────

    /**
     * Confirma o e-mail do cliente a partir do token enviado por e-mail no cadastro.
     * Marca o token como usado e redireciona para o login após verificação bem-sucedida.
     */
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

    /** Endpoint exclusivo para ambiente local que verifica o e-mail sem precisar do link. */
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

    /** Registra um novo cliente via API, gera token JWT e define o cookie de sessão automaticamente. */
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