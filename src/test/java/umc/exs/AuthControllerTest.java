package umc.exs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.backstage.security.JwtUtil;
import umc.exs.controller.prod.AuthController;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.entidades.usuario.Cliente;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)  // Usar MockitoExtension para integração com JUnit 5
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    private final ObjectMapper om = new ObjectMapper();

    private String email;
    private String password;

    @BeforeEach
    public void setup() {
        // Gera um e-mail único a cada execução
        email = "testuser-" + System.currentTimeMillis() + "@example.com";
        password = "Senha@123";
    }

    @Test
    public void testRegisterAndLogin_Success() throws Exception {
        // ---------- REGISTRO ----------

        Map<String, Object> signupPayload = Map.of(
            "email", email,
            "senha", password,
            "password", password,
            "nome", "Teste Integracao",
            "datanasc", "1990-01-01",
            "gen", "M",
            "termsAccepted", true,
            "privacyAccepted", true
        );

        when(clienteRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(42L);
            return c;
        });

        mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(signupPayload)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.message").exists());

        // ---------- LOGIN ----------

        Map<String, Object> loginPayload = Map.of(
            "email", email,
            "senha", password,
            "password", password
        );

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtUtil.generateToken(email)).thenReturn("jwt-xyz-123");

        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(loginPayload)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void testLogin_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        Map<String, Object> loginPayload = Map.of(
            "email", "naoexiste@example.com",
            "senha", "senhaerrada",
            "password", "senhaerrada"
        );

        // Make authenticationManager throw to simulate invalid credentials
        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("bad credentials"));

        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(loginPayload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid credentials"));
    }

    @Test
    public void register_success_usesSignupDTO_and_returnsCreated() throws Exception {
        String regEmail = "reg+" + System.currentTimeMillis() + "@example.com";
        Map<String, Object> body = Map.of(
            "nome", "Teste",
            "email", regEmail,
            "password", "P@ssw0rd",
            "senha", "P@ssw0rd",
            "datanasc", "1990-01-01",
            "gen", "M",
            "termsAccepted", true,
            "privacyAccepted", true
        );

        when(clienteRepository.findByEmail(regEmail)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        // simulate save returns Cliente with id
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(123L);
            return c;
        });

        mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.status").exists());

        // ensure repository.save received mapped Cliente (mapper used in controller)
        ArgumentCaptor<Cliente> cap = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(cap.capture());
        Cliente saved = cap.getValue();
        assert saved.getEmail().equals(regEmail);
    }

    @Test
    public void login_success_returnsToken_usingLoginDTO() throws Exception {
        String loginEmail = "login+" + System.currentTimeMillis() + "@example.com";
        Map<String, Object> body = Map.of("email", loginEmail, "senha", "MyPass123", "password", "MyPass123");

        // mock AuthenticationManager to not throw and JwtUtil to return token
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtUtil.generateToken(loginEmail)).thenReturn("jwt-token-xyz");

        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.token").value("jwt-token-xyz"));
    }
}
