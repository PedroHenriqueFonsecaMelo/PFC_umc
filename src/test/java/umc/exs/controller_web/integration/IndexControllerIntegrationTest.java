package umc.exs.controller_web.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.controller.web.IndexController;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;
import umc.exs.service.core.interactions.VisitaSiteService;

@WebMvcTest(controllers = IndexController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class })
class IndexControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private VisitaSiteService visitaSiteService;

    @MockitoBean
    private LivroRepository livroRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private PedidoRepository pedidoRepository;

    @Test
    void exibirPaginaEntrarRetornaViewEntrar() throws Exception {
        mockMvc.perform(get("/entrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("entrar"));
    }

    @Test
    void postarEntrarAdminRedirecionaPainel() throws Exception {
        UserDetails admin = User.withUsername("admin@example.com")
                .password("encoded-password")
                .authorities("ADMIN")
                .build();

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(admin);
        when(passwordEncoder.matches("senha", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("admin@example.com")).thenReturn("token-value");

        mockMvc.perform(post("/entrar")
                .param("email", "admin@example.com")
                .param("senha", "senha")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/painel"));
    }

    @Test
    void postarEntrarClienteInvalidoExibePaginaEntrar() throws Exception {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("Usuário não encontrado"));

        when(clienteService.autenticarCliente(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Usuário ou senha inválidos"));

        mockMvc.perform(post("/entrar")
                .param("email", "client@example.com")
                .param("senha", "senha incorreta")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("entrar"));
    }
}