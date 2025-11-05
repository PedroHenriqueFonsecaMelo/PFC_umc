package umc.exs.controller.prod;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.backstage.security.JwtUserDetailsService;
import umc.exs.backstage.security.JwtUtil;
import umc.exs.backstage.service.ClienteService;
import umc.exs.model.dtos.auth.LoginDTO;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.ClienteDTO;

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

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Cria e adiciona o cookie JWT à resposta.
     */
    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(false) // alterar para true em produção (HTTPS)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // ============================================================
    // LOGIN
    // ============================================================

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

    // ============================================================
    // REGISTRO
    // ============================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupDTO signupDTO, HttpServletResponse response) {
        try {
            // criptografa a senha antes de salvar
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
