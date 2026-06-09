package umc.exs.controller_api.unitary.compras;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import umc.exs.controller.api.compras.TokenControllerApi;
import umc.exs.design.strategy.impl.PagamentoPixStrategy;
import umc.exs.dto.request.compra.CompraTokensRequest;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.security.JwtRequestFilter;
import umc.exs.security.JwtUtil;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.interactions.VisitaSiteService;

@WebMvcTest(TokenControllerApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TokenControllerApiTest.SecurityConfigTest.class)
@TestPropertySource(properties = {
        "mercadopago.access-token=test-token"
})
class TokenControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private PagamentoPixStrategy pixStrategy;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private VisitaSiteService visitaSiteService;

    @TestConfiguration
    static class SecurityConfigTest {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

            return http.build();
        }
    }

    @Test
    @WithMockUser(username = "teste@email.com")
    void comprar_deveRetornarCreated() throws Exception {

        CompraTokensRequest req = new CompraTokensRequest();
        req.setValor(10.0);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("teste@email.com");

        when(clienteService.buscarEntidadePorEmail(anyString()))
                .thenReturn(cliente);

        when(pixStrategy.processar(anyDouble(), any()))
                .thenReturn(true);

        doNothing().when(clienteService)
                .registrarTransacaoPendente(anyLong(), anyDouble(), anyString());

        mockMvc.perform(post("/api/tokens/comprar")
                        .with(user("teste@email.com"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "teste@email.com")
    void historico_deveRetornarLista() throws Exception {

        when(clienteService.listarHistoricoTransacoes(anyString()))
                .thenReturn(List.of(new Transacao()));

        mockMvc.perform(get("/api/tokens/historico")
                        .with(user("teste@email.com")))
                .andExpect(status().isOk());
    }

    @Test
    void verificarPagamento_deveRetornarPendente() throws Exception {

        when(clienteService.verificarSeFoiPago(anyString()))
                .thenReturn(false);

        mockMvc.perform(get("/api/tokens/verificar-pagamento/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void verificarPagamento_deveRetornarAprovado() throws Exception {

        when(clienteService.verificarSeFoiPago(anyString()))
                .thenReturn(true);

        mockMvc.perform(get("/api/tokens/verificar-pagamento/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void webhook_deveRetornarOk() throws Exception {

        Map<String, Object> body = Map.of(
                "type", "payment",
                "data", Map.of("id", "123")
        );

        mockMvc.perform(post("/api/tokens/webhook")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void simularWebhook_deveRetornarMensagem() throws Exception {

        doNothing().when(clienteService)
                .aprovarPagamento(anyString());

        mockMvc.perform(get("/api/tokens/simular-webhook/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem")
                        .value("Pagamento aprovado via simulação!"));
    }
}