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

import umc.exs.backstage.service.FieldValidation;
import umc.exs.model.DAO.ClienteMapper;
import umc.exs.model.DTO.LoginDTO;
import umc.exs.model.DTO.SignupDTO;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;
import umc.exs.security.JwtUtil;

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

    /**
     * Registra um novo cliente no sistema.
     * Valida e-mail, nome e aceitação dos termos antes de criar a conta.
     * Retorna HTTP 201 (Created) em caso de sucesso.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupDTO dto) {
        String email = FieldValidation.sanitizeEmail(dto.getEmail());
        String nome = FieldValidation.sanitize(dto.getNome());

        if (email == null || nome == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid input"));
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
        c.setSenha(passwordEncoder.encode(dto.getPassword()));
        clienteRepository.save(c);

        // ✅ Retorna CREATED (201) conforme esperado no teste
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Client created successfully"));
    }

    /**
     * Realiza o login do cliente.
     * Autentica o e-mail e senha e retorna um token JWT válido.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDto) {
        try {
            String email = FieldValidation.sanitizeEmail(loginDto.getEmail());
            String password = FieldValidation.sanitize(loginDto.getPassword());

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "invalid input"));
            }

            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            String token = jwtUtil.generateToken(email);

            return ResponseEntity.ok(Map.of("token", token));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid credentials"));
        }
    }

}
