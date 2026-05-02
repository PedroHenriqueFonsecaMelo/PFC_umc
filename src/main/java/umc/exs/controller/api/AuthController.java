package umc.exs.controller.api;

import java.util.Map;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.ClienteService;

/**
 * REST API de autenticação — /auth/login e /auth/register.
 * Toda a lógica de brute-force e validação está encapsulada no ClienteService.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final ClienteService clienteService;

    // ── LOGIN ────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginDTO loginDto,
            HttpServletResponse response) {
        try {
            return clienteService.autenticarCliente(loginDto.getEmail(), loginDto.getSenha())
                    .map(cliente -> {
                        String token = jwtUtil.generateToken(cliente.getEmail());
                        addTokenCookie(response, token);
                        log.info("Login via API bem-sucedido: {}", cliente.getEmail());
                        return ResponseEntity.ok(Map.of(
                                "message", "Login bem-sucedido",
                                "token", token));
                    })
                    .orElseGet(() -> ResponseEntity.status(401)
                            .body(Map.of("error", "E-mail ou senha inválidos.")));

        } catch (IllegalArgumentException e) {
            // Conta bloqueada ou regra de negócio
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro interno no login: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao efetuar login."));
        }
    }

    // ── REGISTER ─────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody SignupDTO signupDTO,
            HttpServletResponse response) {
        try {
            // ClienteService.salvarCliente já faz o encode da senha internamente
            ClienteDTO clienteSalvo = clienteService.salvarCliente(signupDTO);

            String token = jwtUtil.generateToken(clienteSalvo.getEmail());
            addTokenCookie(response, token);

            log.info("Novo cliente registrado via API: {}", clienteSalvo.getEmail());
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Cliente registrado com sucesso",
                    "token", token,
                    "cliente", clienteSalvo));

        } catch (IllegalArgumentException e) {
            // Email duplicado, CPF inválido, menor de idade, etc.
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao registrar cliente: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao processar registro."));
        }
    }

    // ── COOKIE ───────────────────────────────────────────────────────
    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
