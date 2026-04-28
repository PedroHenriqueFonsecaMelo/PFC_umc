package umc.exs.controller_web.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.controller.web.LivroViewController;
import umc.exs.security.JwtUtil;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.service.core.interactions.VisitaSiteService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LivroViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class LivroViewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisitaSiteService visitaSiteService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    @Test
    void paginaVenderRetornaViewVendaLivro() throws Exception {
        mockMvc.perform(get("/livros/vender"))
                .andExpect(status().isOk())
                .andExpect(view().name("produto/venda_livro"));
    }

    @Test
    void paginaVitrineRetornaViewVitrineLivros() throws Exception {
        mockMvc.perform(get("/livros/vitrine"))
                .andExpect(status().isOk())
                .andExpect(view().name("produto/vitrine_livros"));
    }
}
