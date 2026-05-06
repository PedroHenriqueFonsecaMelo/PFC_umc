package umc.exs.controller_web.unitary;

import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.controller.web.IndexController;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IndexControllerUnitTest {

    private JwtUserDetailsService userDetailsService;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private ClienteService clienteService;
    private AuthHelper authHelper;
    private IndexController controller;

    @BeforeEach
    void setUp() {
        userDetailsService = mock(JwtUserDetailsService.class);
        jwtUtil = mock(JwtUtil.class);
        passwordEncoder = mock(PasswordEncoder.class);
        clienteService = mock(ClienteService.class);
        authHelper = mock(AuthHelper.class);
        controller = new IndexController(userDetailsService, jwtUtil, passwordEncoder, clienteService, authHelper);
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarIndex() {
        assertEquals("index", controller.index());
    }

    @Test
    void deveRedirecionarAdminQuandoCredenciaisValidas() {
        UserDetails admin = User.withUsername("admin@example.com")
                .password("encoded-password")
                .authorities("ADMIN")
                .build();

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(admin);
        when(passwordEncoder.matches("senha", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("admin@example.com")).thenReturn("token-value");

        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = controller.processarLogin("admin@example.com", "senha", model, response);

        assertEquals("redirect:/admin/painel", result);
        verify(jwtUtil).addTokenCookie(response, "token-value");
    }

    @Test
    void deveRedirecionarRootQuandoClienteValido() {
        when(userDetailsService.loadUserByUsername("cliente@example.com")).thenThrow(new UsernameNotFoundException("Not found"));

        ClienteDTO cliente = new ClienteDTO();
        cliente.setId(42L);
        cliente.setEmail("cliente@example.com");
        when(clienteService.autenticarCliente("cliente@example.com", "senha"))
                .thenReturn(Optional.of(cliente));

        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = controller.processarLogin("cliente@example.com", "senha", model, response);

        assertEquals("redirect:/", result);
        verify(authHelper).authenticateAndSetCookie("cliente@example.com", 42L, response, "LOGIN_SUCESSO");
    }

    @Test
    void deveRetornarEntrarQuandoCredenciaisInvalidas() {
        when(userDetailsService.loadUserByUsername("erro@example.com")).thenThrow(new UsernameNotFoundException("Not found"));
        when(clienteService.autenticarCliente("erro@example.com", "senha")).thenReturn(Optional.empty());

        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = controller.processarLogin("erro@example.com", "senha", model, response);

        assertEquals("entrar", result);
        assertEquals("E-mail ou senha inválidos.", model.asMap().get("erro"));
        assertEquals("erro@example.com", model.asMap().get("emailAnterior"));
    }
}
