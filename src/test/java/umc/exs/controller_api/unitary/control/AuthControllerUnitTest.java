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
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.model.entidades.foundation.EmailVerificacao;
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

        controller = new AuthController(jwtUtil, clienteService, authHelper, emailVerificacaoRepository,
                clienteRepository);
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
    void login_InvalidCredentials_RetornaRedirecionamento() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@email.com");
        req.setSenha("123");

        when(clienteService.autenticarCliente(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("invalid"));

        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<?> resp = controller.login(req, response, request);

        assertEquals(HttpStatus.FOUND, resp.getStatusCode());

        verifyNoInteractions(authHelper);
    }

    @Test
    void logout_Sucesso() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getCookies()).thenReturn(null);

        ResponseEntity<Map<String, Object>> resp = controller.logout(request, response);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Logout realizado com sucesso.", resp.getBody().get("mensagem"));
        verify(response).addCookie(any());
    }

    @Test
    void verificarEmail_TokenInvalido() {
        when(emailVerificacaoRepository.findByToken("abc")).thenReturn(java.util.Optional.empty());

        ResponseEntity<Map<String, Object>> resp = controller.verificarEmail("abc");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Token inválido ou já utilizado.", resp.getBody().get("erro"));
    }

    @Test
    void verificarEmail_TokenExpirado() {
        EmailVerificacao ver = mock(EmailVerificacao.class);

        when(ver.isUsado()).thenReturn(false);
        when(ver.isExpirado()).thenReturn(true);
        when(emailVerificacaoRepository.findByToken("abc")).thenReturn(java.util.Optional.of(ver));

        ResponseEntity<Map<String, Object>> resp = controller.verificarEmail("abc");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Token expirado. Solicite novo link.", resp.getBody().get("erro"));
    }

    @Test
    void verificarEmail_Sucesso() {
        EmailVerificacao ver = mock(EmailVerificacao.class);
        Cliente cliente = mock(Cliente.class);

        when(ver.isUsado()).thenReturn(false);
        when(ver.isExpirado()).thenReturn(false);
        when(ver.getCliente()).thenReturn(cliente);
        when(emailVerificacaoRepository.findByToken("abc")).thenReturn(java.util.Optional.of(ver));

        ResponseEntity<Map<String, Object>> resp = controller.verificarEmail("abc");

        assertEquals(HttpStatus.FOUND, resp.getStatusCode());
        verify(cliente).setEmailVerificado(true);
        verify(clienteRepository).save(cliente);
        verify(ver).setUsado(true);
        verify(emailVerificacaoRepository).save(ver);
    }

    @Test
    void devVerificarEmail_ClienteNaoEncontrado() {
        when(clienteRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        ResponseEntity<Map<String, Object>> resp = controller.devVerificarEmail(1L);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void devVerificarEmail_Sucesso() {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getEmail()).thenReturn("user@email.com");

        when(clienteRepository.findById(1L)).thenReturn(java.util.Optional.of(cliente));

        ResponseEntity<Map<String, Object>> resp = controller.devVerificarEmail(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Email verificado (modo dev)", resp.getBody().get("mensagem"));

        verify(cliente).setEmailVerificado(true);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void register_Sucesso() {
        SignupRequest req = new SignupRequest();
        req.setEmail("user@email.com");

        Cliente cliente = mock(Cliente.class);
        when(cliente.getEmail()).thenReturn("user@email.com");

        when(clienteService.salvarCliente(any())).thenReturn(cliente);
        when(jwtUtil.generateToken("user@email.com")).thenReturn("token-abc");

        HttpServletResponse response = mock(HttpServletResponse.class);

        ResponseEntity<Map<String, Object>> resp = controller.register(req, response);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals("Cliente registrado com sucesso", resp.getBody().get("message"));
        assertEquals("token-abc", resp.getBody().get("token"));

        verify(authHelper).addTokenCookie(response, "token-abc");
    }

}
