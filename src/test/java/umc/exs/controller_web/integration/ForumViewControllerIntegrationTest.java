package umc.exs.controller_web.integration;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.controller.web.ForumViewController;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.security.JwtUtil;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.service.core.interactions.ForumService;
import umc.exs.service.core.interactions.VisitaSiteService;
import umc.exs.repository.usuario.ClienteRepository;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ForumViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ForumViewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ForumService forumService;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private VisitaSiteService visitaSiteService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    @SuppressWarnings("null")
    @Test
    void listarForumRetornaViewLista() throws Exception {
        Page<TopicoForum> page = new PageImpl<>(List.of());
        // Ajustado para bater com qualquer PageRequest para evitar erros de comparação
        // de objeto
        when(forumService.listarTopicos(any(), any(), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/forum"))
                .andExpect(status().isOk())
                .andExpect(view().name("forum/lista"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void verTopicoRetornaViewTopico() throws Exception {
        // 1. Criar o Cliente (Autor) com dados mínimos
        Cliente autorMock = new Cliente();
        autorMock.setId(1L);
        autorMock.setNome("Usuário Teste");
        autorMock.setFotoPerfil("foto.jpg");

        // 2. Criar o Tópico e GARANTIR a associação
        TopicoForum topico = new TopicoForum();
        topico.setId(1L);
        topico.setTitulo("Título de Teste");
        topico.setConteudo("Conteúdo de Teste");
        topico.setCategoria(CategoriaForum.GERAL);
        topico.setAutor(autorMock); // Aqui o objeto autorMock DEVE estar presente

        // 3. Ajustar os Mocks para serem mais flexíveis
        // As vezes o ID passado no teste pode sofrer cast ou o controller usa um
        // wrapper
        when(forumService.buscarTopicoPorId(anyLong())).thenReturn(topico);

        // Simular que o usuário logado existe
        when(clienteRepository.findByEmail(anyString())).thenReturn(Optional.of(autorMock));

        // Mock de likes (vazio)
        when(forumService.getRespostasLikedByUser(anyLong(), anyLong())).thenReturn(Set.of());

        // 4. Executar e validar
        mockMvc.perform(get("/forum/topicos/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("forum/topico"));
    }
}