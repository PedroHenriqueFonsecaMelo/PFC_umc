package umc.exs.controller_web.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.controller.web.AdminViewController;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.interactions.VisitaSiteService;
import umc.exs.service.log.LogAuditoriaService;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminViewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private LogAuditoriaService logAuditoriaService;

    @MockitoBean
    private VisitaSiteService visitaSiteService;

    @Test
    void exibirLoginAdminRetornaViewAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin_login"));
    }

    @SuppressWarnings("null")
    @Test
    void postarLoginAdminValidoRedirecionaPainel() throws Exception {
        UserDetails admin = User.withUsername("admin@example.com")
                .password("encoded-password")
                .authorities("ADMIN")
                .build();

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(admin);
        when(passwordEncoder.matches("senha", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("admin@example.com")).thenReturn("token-value");

        mockMvc.perform(post("/admin/login")
                        .param("email", "admin@example.com")
                        .param("senha", "senha")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/painel"));
    }
}
