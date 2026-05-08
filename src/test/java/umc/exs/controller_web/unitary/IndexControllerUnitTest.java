package umc.exs.controller_web.unitary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import umc.exs.controller.web.IndexController;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebMvcTest(IndexController.class)
class IndexControllerUnitTest {

    @Autowired
    private IndexController controller;

    @MockitoBean
    private JwtUserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private AuthHelper authHelper;

    @MockitoBean
    private LivroRepository livroRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private PedidoRepository pedidoRepository;

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarIndexComEstatisticas() {

        Model model = new ExtendedModelMap();

        when(livroRepository.countByAprovadoTrue()).thenReturn(10L);
        when(clienteRepository.count()).thenReturn(5L);
        when(pedidoRepository.count()).thenReturn(2L);

        String view = controller.index(model);

        assertEquals("index", view);
        assertEquals(10L, model.asMap().get("statLivros"));
        assertEquals(5L, model.asMap().get("statLeitores"));
        assertEquals(2L, model.asMap().get("statTrocas"));
    }

    @Test
    void deveRedirecionarEntrarParaLogin() {

        String view = controller.entrar();

        assertEquals("redirect:/clientes/login", view);
    }

    @Test
    void deveRetornarShop() {
        assertEquals("shop", controller.shop());
    }

    @Test
    void deveRetornarLoginPage() {
        assertEquals("login", controller.loginPage());
    }

    @Test
    void deveRetornarAdminPage() {
        assertEquals("admin", controller.adminPage());
    }
}