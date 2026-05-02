package umc.exs.controller_web.integration;

// IMPORTS ESSENCIAIS
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.controller.web.AuditController;
import umc.exs.model.entidades.logic.LogAuditoria;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.interactions.VisitaSiteService;
import umc.exs.service.log.LogAuditoriaService;

@WebMvcTest(AuditController.class)
class AuditControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext context; // Adicione isso

        @MockitoBean
        private ClienteService clienteService;

        @MockitoBean
        private LogAuditoriaService logAuditoriaService;

        @MockitoBean
        private VisitaSiteService visitaSiteService;

        @MockitoBean
        private JwtUtil jwtUtil;

        @MockitoBean
        private JwtUserDetailsService jwtUserDetailsService;

        @SuppressWarnings("null")
        @BeforeEach
        void setup() {
                // Isso garante que o MockMvc respeite as configurações de segurança e usuários
                // mockados
                this.mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        @WithMockUser(username = "user@example.com")
        void listarAuditoriaClienteRetornaView() throws Exception {
                ClienteDTO clienteDTO = new ClienteDTO();
                clienteDTO.setId(10L);
                clienteDTO.setEmail("user@example.com");

                // Use matchers genéricos para evitar erros de tipagem/conversão de String
                when(clienteService.buscarClientePorEmail(anyString()))
                                .thenReturn(Optional.of(clienteDTO));

                when(logAuditoriaService.buscarLogsDoCliente(anyLong()))
                                .thenReturn(List.of(new LogAuditoria()));

                mockMvc.perform(get("/historico/cliente"))
                                .andDo(print()) // Isso vai mostrar no console para onde o 302 está apontando
                                .andExpect(status().isOk())
                                .andExpect(view().name("cliente/auditoria"));
        }
}