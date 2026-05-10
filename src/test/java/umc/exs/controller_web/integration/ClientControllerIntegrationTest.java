package umc.exs.controller_web.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.controller.web.ClientController;
import umc.exs.dtos.user.ClienteDTO;
import umc.exs.security.JwtUtil;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;
import umc.exs.service.core.interactions.VisitaSiteService;
import umc.exs.service.gamificacao.GamificacaoService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collection;
import java.util.List;

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

        @MockitoBean
        private GamificacaoService gamificacaoService;

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
        void postarLoginClienteValidoRedirecionaHomepage() throws Exception {

                ClienteDTO cliente = new ClienteDTO();
                cliente.setId(1L);
                cliente.setEmail("client@example.com");

                when(clienteService.autenticarCliente(anyString(), anyString()))
                                .thenReturn(cliente);

                doNothing().when(authHelper)
                                .authenticateAndSetCookie(anyString(), anyLong(), any(), any());

                UserDetails userDetails = mock(UserDetails.class);
                when(userDetails.getAuthorities())
                                .thenReturn((Collection) List.of(new SimpleGrantedAuthority("CLIENTE")));

                when(jwtUserDetailsService.loadUserByUsername(anyString()))
                                .thenReturn(userDetails);

                mockMvc.perform(post("/clientes/login")
                                .param("email", "client@example.com")
                                .param("senha", "senha123")
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/clientes/homepage"));

                verify(authHelper).authenticateAndSetCookie(
                                eq("client@example.com"),
                                eq(1L),
                                any(),
                                any());
        }
}
