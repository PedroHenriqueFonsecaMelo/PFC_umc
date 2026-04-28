package umc.exs.controller_web.unitary;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.controller.web.AdminViewController;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.log.LogAuditoriaService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminViewControllerUnitTest {

    private JwtUserDetailsService userDetailsService;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private LogAuditoriaService logAuditoriaService;
    private AdminViewController controller;

    @BeforeEach
    void setUp() {
        userDetailsService = mock(JwtUserDetailsService.class);
        jwtUtil = mock(JwtUtil.class);
        passwordEncoder = mock(PasswordEncoder.class);
        logAuditoriaService = mock(LogAuditoriaService.class);
        controller = new AdminViewController(userDetailsService, jwtUtil, passwordEncoder, logAuditoriaService);
    }

    @Test
    void deveExibirPaginaLoginAdmin() {
        Model model = new ExtendedModelMap();

        String view = controller.loginPage(model);

        assertEquals("admin/admin_login", view);
        assertTrue(model.containsAttribute("loginData"));
    }

    @Test
    void deveRedirecionarPainelQuandoCredenciaisAdminValidas() {
        UserDetails admin = User.withUsername("admin@example.com")
                .password("encoded-password")
                .authorities("ADMIN")
                .build();

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(admin);
        when(passwordEncoder.matches("senha", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("admin@example.com")).thenReturn("token-value");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("admin@example.com");
        loginDTO.setSenha("senha");

        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        String view = controller.processLogin(loginDTO, "admin@example.com", "senha", model, response);

        assertEquals("redirect:/admin/painel", view);
        verify(jwtUtil).addTokenCookie(response, "token-value");
        assertEquals("token-value", model.asMap().get("token"));
    }

    @Test
    void deveRetornarLoginQuandoSenhaInvalida() {
        UserDetails admin = User.withUsername("admin@example.com")
                .password("encoded-password")
                .authorities("ADMIN")
                .build();

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(admin);
        when(passwordEncoder.matches("senhaErrada", "encoded-password")).thenReturn(false);

        LoginDTO loginDTO = new LoginDTO();
        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        String view = controller.processLogin(loginDTO, "admin@example.com", "senhaErrada", model, response);

        assertEquals("admin/admin_login", view);
        assertEquals("E-mail ou senha inválidos.", model.asMap().get("erro"));
    }
}
