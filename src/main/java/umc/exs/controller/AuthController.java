package umc.exs.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import umc.exs.backstage.security.JwtUtil;
import umc.exs.backstage.service.FieldValidation;
import umc.exs.model.DAO.ClienteMapper;
import umc.exs.model.DTO.auth.LoginDTO;
import umc.exs.model.DTO.auth.SignupDTO;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private void addAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true em produção
        cookie.setMaxAge(24 * 60 * 60); // 1 dia
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * Realiza o login do cliente.
     * Autentica o e-mail e senha e retorna um token JWT válido.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDto, HttpServletResponse response) {
        try {
            String email = FieldValidation.sanitizeEmail(loginDto.getEmail());
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, loginDto.getPassword()));
            
            final String token = jwtUtil.generateToken(email);
            addAuthCookie(response, token);
            
            return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer"
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    /**
     * Registra um novo cliente no sistema.
     * Valida e-mail, nome e aceitação dos termos antes de criar a conta.
     * Retorna HTTP 201 (Created) em caso de sucesso.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupDTO dto, HttpServletResponse response) {
        String email = FieldValidation.sanitizeEmail(dto.getEmail());
        String nome = FieldValidation.sanitize(dto.getNome());

        if (email == null || nome == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        if (!FieldValidation.isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
        }

        if (dto.getTermsAccepted() == null || !dto.getTermsAccepted()
                || dto.getPrivacyAccepted() == null || !dto.getPrivacyAccepted()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "terms and privacy required"));
        }

        if (clienteRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "email already registered"));
        }

        Cliente c = ClienteMapper.toEntityFromSignup(dto);
        c.setSenha(passwordEncoder.encode(dto.getSenha()));
        clienteRepository.save(c);

        // Auto-login após registro
        final String token = jwtUtil.generateToken(c.getEmail());
        addAuthCookie(response, token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "Registration successful",
                    "token", token
                ));
    }
}
