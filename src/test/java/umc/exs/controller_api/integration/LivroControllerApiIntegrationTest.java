package umc.exs.controller_api.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.service.core.bussiness.LivroService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LivroControllerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LivroService livroService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("null")
    @Test
    void comprarCarrinho_SemAuth_ReturnsUnauthorized() throws Exception {
        CarrinhoCompraRequest request = new CarrinhoCompraRequest();
        request.setLivroIds(List.of(1L));

        mockMvc.perform(post("/api/livros/carrinho/comprar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @SuppressWarnings("null")
    @Test
    void comprarCarrinho_ComAuth_ReturnsOk() throws Exception {

        CarrinhoCompraRequest request = new CarrinhoCompraRequest();
        request.setLivroIds(List.of(1L));

        CarrinhoCompraResponse response = new CarrinhoCompraResponse();
        response.setComprados(List.of());
        response.setFalhas(List.of());

        when(livroService.comprarCarrinho(anyString(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/livros/carrinho/comprar")
                .with(user("user@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}