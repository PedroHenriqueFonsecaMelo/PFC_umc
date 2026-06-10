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
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.security.JwtUtil;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;
import umc.exs.dto.mapper.ClienteMapper;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.service.gamificacao.GamificacaoService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

class ClientControllerUnitTest {

        private ClienteService clienteService;
        private AuthHelper authHelper;
        private JwtUtil jwtUtil;
        private JwtUserDetailsService userDetailsService;
        private PasswordEncoder passwordEncoder;
        private GamificacaoService gamificacaoService;
        private ClienteMapper clienteMapper;
        private ClienteRepository clienteRepository;

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
                clienteRepository = mock(ClienteRepository.class);

                controller = new ClientController(
                                clienteService,
                                authHelper,
                                jwtUtil,
                                userDetailsService,
                                passwordEncoder,
                                gamificacaoService,
                                clienteRepository,
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

        @Test
        void deveRetornarCadastroQuandoSenhaNaoConfere() {
                SignupRequest dto = new SignupRequest();
                dto.setSenha("123");
                dto.setConfirmPassword("456");

                BindingResult result = new BeanPropertyBindingResult(dto, "cliente");
                Model model = new ExtendedModelMap();
                HttpServletResponse response = mock(HttpServletResponse.class);

                String view = controller.registrarCliente(dto, result, "456", model, response);

                assertEquals("cliente/cadastro_cliente", view);
        }

        @Test
        void deveExibirLoginComObjetoNovo() {
                Model model = new ExtendedModelMap();

                String view = controller.exibirLogin(model);

                assertEquals("cliente/login_cliente", view);
                assertTrue(model.containsAttribute("loginData"));
        }

        @Test
        void deveRedirecionarParaLoginSeHomepageSemUsuario() {
                String view = controller.exibirHomepage(null, new ExtendedModelMap());

                assertEquals("redirect:/clientes/login", view);
        }

        @Test
        void deveExibirHomepageComUsuario() {
                var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
                when(user.getUsername()).thenReturn("email@test.com");

                Cliente cliente = new Cliente();
                cliente.setEmail("email@test.com");

                when(clienteService.buscarClientePorEmail("email@test.com"))
                                .thenReturn(Optional.of(cliente));

                Model model = new ExtendedModelMap();

                String view = controller.exibirHomepage(user, model);

                assertEquals("cliente/homepage", view);
                assertTrue(model.containsAttribute("cliente"));
        }

        @Test
        void deveFazerLogout() {
                HttpServletResponse response = mock(HttpServletResponse.class);

                String view = controller.deslogar(response, null);

                assertEquals("redirect:/?logout=true", view);

                verify(jwtUtil).clearJwtCookie(response);
        }

        @Test
        void deveRetornarPerfilJsonQuandoUsuarioNaoAutenticado() {
                var response = controller.perfilJson(null);

                assertEquals(401, response.getStatusCode().value());
        }

        @Test
        void deveRetornarPerfilJsonComSucesso() {
                var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
                when(user.getUsername()).thenReturn("email@test.com");

                Cliente cliente = new Cliente();

                when(clienteService.buscarClientePorEmail("email@test.com"))
                                .thenReturn(Optional.of(cliente));

                when(clienteMapper.toPerfilResponse(cliente))
                                .thenReturn(mock(umc.exs.dto.response.cliente.ClientePerfilResponse.class));

                var response = controller.perfilJson(user);

                assertEquals(200, response.getStatusCode().value());
        }

        @Test
        void deveExibirPaginaCancelamento() {
                var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
                Model model = new ExtendedModelMap();

                String view = controller.paginaCancelamento(1L, user, model);

                assertEquals("cliente/cancelamento", view);
                assertEquals(1L, model.getAttribute("pedidoId"));
        }

        @Test
        void deveRedirecionarCancelamentoSemUsuario() {
                String view = controller.paginaCancelamento(1L, null, new ExtendedModelMap());

                assertEquals("redirect:/clientes/login", view);
        }

        @Test
        void deveAtualizarClienteComSucesso() {
                var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
                when(user.getUsername()).thenReturn("email@test.com");

                var ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

                String view = controller.atualizarCliente(
                                new umc.exs.dto.request.cliente.ClienteUpdateRequest(),
                                user,
                                ra);

                assertEquals("redirect:/clientes/meu-perfil", view);

                verify(clienteService).atualizarDadosLogados(eq("email@test.com"), any());
        }

        @Test
        void deveDeletarConta() {
                var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
                when(user.getUsername()).thenReturn("email@test.com");

                HttpServletResponse response = mock(HttpServletResponse.class);
                var ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

                String view = controller.deletarConta(user, response, ra);

                assertEquals("redirect:/", view);

                verify(clienteService).deletarContaPropria("email@test.com");
                verify(jwtUtil).clearJwtCookie(response);
        }

        @Test
        void deveExibirCarteira() {
                var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
                when(user.getUsername()).thenReturn("email@test.com");

                Cliente cliente = new Cliente();
                cliente.setEmail("email@test.com");

                when(clienteService.buscarClientePorEmail("email@test.com"))
                                .thenReturn(Optional.of(cliente));

                when(clienteService.listarHistoricoTransacoes("email@test.com"))
                                .thenReturn(List.of());

                Model model = new ExtendedModelMap();

                String view = controller.exibirCarteira(user, model);

                assertEquals("cliente/homepage", view);
                assertTrue(model.containsAttribute("cliente"));
                assertTrue(model.containsAttribute("historico"));
        }

        @Test
        void deveComprarTokens() {
                var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
                when(user.getUsername()).thenReturn("email@test.com");

                var ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

                String view = controller.comprarTokens(100.0, user, ra);

                assertEquals("redirect:/clientes/carteira", view);

                verify(clienteService).adicionarTokensParaUsuarioLogado("email@test.com", 100.0);
        }

        @Test
        void deveMostrarPaginaRecuperarSenha() {
                String view = controller.mostrarPaginaRecuperarSenha();

                assertEquals("cliente/recuperar_senha", view);
        }

        @Test
        void deveIniciarRecuperacaoSenha() {
                var ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

                String view = controller.iniciarRecuperacaoSenha("email@test.com", ra);

                assertEquals("redirect:/clientes/login", view);

                verify(clienteService).iniciarRecuperacaoSenha("email@test.com");
        }

        @Test
        void deveMostrarTermo() {
                assertEquals("cliente/Termo", controller.mostrarTermo());
        }

        @Test
        void deveMostrarPolitica() {
                assertEquals("cliente/Politica", controller.mostrarPolitica());
        }

        @Test
        void deveMostrarSobre() {
                assertEquals("cliente/Sobre", controller.mostrarSobre());
        }
}