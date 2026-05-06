package umc.exs.controller_web.integration;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.controller.web.ClientController;
import umc.exs.security.JwtUtil;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;
import umc.exs.service.core.interactions.VisitaSiteService;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private AuthHelper authHelper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VisitaSiteService visitaSiteService;

    @Test
    void exibirNovoCadastroRetornaViewCadastroCliente() throws Exception {
        mockMvc.perform(get("/clientes/novo-cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/cadastro_cliente"));
    }

    @Test
    void exibirLoginRetornaViewLoginCliente() throws Exception {
        mockMvc.perform(get("/clientes/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/login_cliente"));
    }

    @SuppressWarnings("null")
    @Test
    void postarLoginClienteValidoRedirecionaRoot() throws Exception {
        when(clienteService.autenticarCliente("client@example.com", "senha123"))
                .thenReturn(Optional.of(new ClienteDTO(1L, null, "client@example.com", null, null, null, null, 0.0, null, null, null)));

        mockMvc.perform(post("/clientes/login")
                        .param("email", "client@example.com")
                        .param("senha", "senha123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
