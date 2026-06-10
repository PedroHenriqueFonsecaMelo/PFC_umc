package umc.exs.controller_web.unitary;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.controller.web.NotificacaoViewController;
import umc.exs.dto.request.admin.EmailDisparoRequest;
import umc.exs.dto.response.email.EmailDestinatarioResponse;
import umc.exs.dto.response.email.EmailHistoricoResponse;
import umc.exs.security.JwtRequestFilter;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.interactions.VisitaSiteService;
import umc.exs.service.email.notificacao.NotificacaoEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NotificacaoViewController.class)
class NotificacaoViewControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private NotificacaoEmailService notificacaoEmailService;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private VisitaSiteService visitaSiteService;

        @MockitoBean
        private JwtRequestFilter jwtRequestFilter;

        @MockitoBean
        private JwtUtil jwtUtil;

        @Test
        void pagina_deveRetornarView() throws Exception {
                mockMvc.perform(get("/admin/notificacoes"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/notificacoes"));
        }

        @Test
        void preview_deveRetornarLista() throws Exception {
                when(notificacaoEmailService.filtrarDestinatarios(anyString(), anyInt()))
                                .thenReturn(List.of(new EmailDestinatarioResponse()));

                mockMvc.perform(get("/admin/notificacoes/preview")
                                .param("filtro", "todos")
                                .param("limite", "1"))
                                .andExpect(status().isOk());
        }

        @Test
        void disparar_deveRetornarMensagem() throws Exception {
                EmailDisparoRequest dto = new EmailDisparoRequest();
                dto.setFiltro("todos");
                dto.setLimite(1);
                dto.setAssunto("Teste");
                dto.setCorpo("Corpo do email de teste");

                when(notificacaoEmailService.dispararOuAgendar(any()))
                                .thenReturn("Enviado");

                mockMvc.perform(post("/admin/notificacoes/disparar")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mensagem").value("Enviado"));
        }

        @Test
        void historico_deveRetornarLista() throws Exception {
                when(notificacaoEmailService.listarHistorico())
                                .thenReturn(List.of(new EmailHistoricoResponse()));

                mockMvc.perform(get("/admin/notificacoes/historico"))
                                .andExpect(status().isOk());
        }
}