package umc.exs.controller.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.ClienteService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private PasswordEncoder passwordEncoder;

/**
 * Adiciona cookie HTTP-only JWT response.
 * Expiração 7 dias, secure false dev, sameSite Lax.
 * @param response para Set-Cookie
 * @param token JWT
 */
private void addTokenCookie(HttpServletResponse response, String token) {


        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

/**
 * Processa login API REST do cliente.
 * Valida credenciais, gera JWT, set cookie e retorna token.
 * Registra falhas via auditoria se inválido.
 * @param loginDto dados email/senha
 * @param response para cookie
 */
@PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDto, HttpServletResponse response) {

        try {
            String email = loginDto.getEmail();
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (userDetails == null || !passwordEncoder.matches(loginDto.getSenha(), userDetails.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Credenciais inválidas"));
            }

            String token = jwtUtil.generateToken(email);
            addTokenCookie(response, token);

            return ResponseEntity.ok(Map.of("message", "Login bem-sucedido", "token", token));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuário não encontrado"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao efetuar login"));
        }
    }

/**
 * Registra novo cliente via API REST.
 * Codifica senha, salva via ClienteService, gera JWT.
 * Retorna cliente + token em caso de sucesso.
 * @param signupDTO dados cadastro
 * @param response para cookie token
 */
@PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupDTO signupDTO, HttpServletResponse response) {

        try {
            signupDTO.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

            ClienteDTO clienteSalvo = clienteService.salvarCliente(signupDTO);
            if (clienteSalvo == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Erro ao cadastrar cliente"));
            }

            String token = jwtUtil.generateToken(clienteSalvo.getEmail());
            addTokenCookie(response, token);

            return ResponseEntity.status(201).body(Map.of(
                    "message", "Cliente registrado com sucesso",
                    "token", token,
                    "cliente", clienteSalvo
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao processar registro"));
        }
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller REST API para autenticação de clientes.
 * Gerencia endpoints /auth/login e /auth/register com JWT tokens em cookies HTTP-only.
 * Integra JwtUtil, ClienteService e PasswordEncoder para segurança.
 * Retorna JSON com token/sucesso ou erros HTTP padronizados.
 */

