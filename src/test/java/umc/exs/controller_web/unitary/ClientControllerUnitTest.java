package umc.exs.controller_web.unitary;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import umc.exs.controller.web.ClientController;
import umc.exs.model.entidades.usuario.Cliente;

import umc.exs.security.JwtUtil;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;
import umc.exs.dto.mapper.ClienteMapper;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.service.gamificacao.GamificacaoService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientControllerUnitTest {

        private ClienteService clienteService;
        private AuthHelper authHelper;
        private JwtUtil jwtUtil;
        private JwtUserDetailsService userDetailsService;
        private PasswordEncoder passwordEncoder;
        private GamificacaoService gamificacaoService;
        private ClienteMapper clienteMapper;

        private ClientController controller;

        @BeforeEach
        void setUp() {
                clienteService = mock(ClienteService.class);
                authHelper = mock(AuthHelper.class);
                jwtUtil = mock(JwtUtil.class);
                userDetailsService = mock(JwtUserDetailsService.class);
                passwordEncoder = mock(PasswordEncoder.class);
                gamificacaoService = mock(GamificacaoService.class);
                clienteMapper = mock(ClienteMapper.class);

                controller = new ClientController(

                                clienteService,
                                authHelper,
                                jwtUtil,
                                userDetailsService,
                                passwordEncoder,
                                gamificacaoService,
                                clienteMapper);

        }

        @Test
        void deveExibirFormularioCadastroComClienteNovo() {
                Model model = new ExtendedModelMap();
                HttpServletResponse response = mock(HttpServletResponse.class);

                String view = controller.exibirFormularioCadastro(response, model);

                assertEquals("cliente/cadastro_cliente", view);
                assertTrue(model.containsAttribute("cliente"));

                verify(jwtUtil).clearJwtCookie(response);
        }

        @Test
        void deveRegistrarClienteComSucesso() {
                SignupRequest dto = new SignupRequest();
                dto.setEmail("client@example.com");
                dto.setSenha("senha123");
                dto.setConfirmPassword("senha123");

                BindingResult result = new BeanPropertyBindingResult(dto, "cliente");
                Model model = new ExtendedModelMap();
                HttpServletResponse response = mock(HttpServletResponse.class);

                Cliente salvo = new Cliente();
                salvo.setId(1L);

                salvo.setEmail("client@example.com");

                when(clienteService.salvarCliente(dto)).thenReturn(salvo);

                String view = controller.registrarCliente(dto, result, "senha123", model, response);

                assertEquals("redirect:/clientes/login?cadastro=ok", view);
        }

        @Test
        void deveRetornarLoginQuandoCredenciaisInvalidas() {
                Model model = new ExtendedModelMap();
                HttpServletResponse response = mock(HttpServletResponse.class);

                when(userDetailsService.loadUserByUsername("client@example.com"))
                                .thenThrow(new UsernameNotFoundException("not found"));

                when(clienteService.autenticarCliente("client@example.com", "senha123"))
                                .thenThrow(new IllegalArgumentException("E-mail ou senha inválidos."));

                String view = controller.realizarLogin(
                                "client@example.com",
                                "senha123",
                                model,
                                response);

                assertEquals("cliente/login_cliente", view);
                assertEquals("E-mail ou senha inválidos.", model.asMap().get("erro"));
        }

        @Test
        void deveFazerLoginClienteComSucesso() {
                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("client@example.com");

                Model model = new ExtendedModelMap();
                HttpServletResponse response = mock(HttpServletResponse.class);

                when(userDetailsService.loadUserByUsername("client@example.com"))
                                .thenThrow(new UsernameNotFoundException("not admin"));

                when(clienteService.autenticarCliente("client@example.com", "senha123"))
                                .thenReturn(cliente);

                String view = controller.realizarLogin(
                                "client@example.com",
                                "senha123",
                                model,
                                response);

                assertEquals("redirect:/clientes/homepage", view);

                verify(authHelper).authenticate(
                                "client@example.com",
                                response);
        }
}