package umc.exs.controller_api.unitary.control;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import umc.exs.controller.api.control.AuthController;
import umc.exs.dto.request.cliente.LoginRequest;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.foundation.EmailVerificacaoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.security.JwtUtil;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;

class AuthControllerUnitTest {

    private JwtUtil jwtUtil;
    private ClienteService clienteService;
    private AuthHelper authHelper;
    private EmailVerificacaoRepository emailVerificacaoRepository;
    private ClienteRepository clienteRepository;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        clienteService = mock(ClienteService.class);
        authHelper = mock(AuthHelper.class);
        emailVerificacaoRepository = mock(EmailVerificacaoRepository.class);
        clienteRepository = mock(ClienteRepository.class);

        controller = new AuthController(jwtUtil, clienteService, authHelper, emailVerificacaoRepository, clienteRepository);
    }

    @Test
    void login_Sucesso_RetornaOk() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@email.com");
        req.setSenha("123");

        Cliente cliente = mock(Cliente.class);
        when(cliente.getEmail()).thenReturn("user@email.com");

        when(clienteService.autenticarCliente("user@email.com", "123")).thenReturn(cliente);
        when(jwtUtil.generateToken("user@email.com")).thenReturn("token-abc");

        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        ResponseEntity<?> resp = controller.login(req, response, request);
        Map<String, Object> body = (Map<String, Object>) resp.getBody();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Login bem-sucedido", body.get("message"));
        assertEquals("token-abc", body.get("token"));
        verify(authHelper).addTokenCookie(response, "token-abc");
    }

    @Test
    void login_InvalidCredentials_Retorna401() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@email.com");
        req.setSenha("123");

        when(clienteService.autenticarCliente(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("invalid"));

        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<?> resp = controller.login(req, response, request);
        Map<String, Object> body = (Map<String, Object>) resp.getBody();

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("E-mail ou senha inválidos.", body.get("error"));
        verifyNoInteractions(authHelper);
    }
}

