package umc.exs.controller_web.unitary;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import umc.exs.DTOs.forum.NovoTopicoDTO;
import umc.exs.controller.web.ForumViewController;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.ForumService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ForumViewControllerUnitTest {

    private ForumService forumService;
    private ClienteRepository clienteRepository;
    private ForumViewController controller;

    @BeforeEach
    void setUp() {
        forumService = mock(ForumService.class);
        clienteRepository = mock(ClienteRepository.class);
        controller = new ForumViewController(forumService, clienteRepository);
    }

    @SuppressWarnings("null")
    @Test
    void deveListarTopicosSemUsuario() {
        // Preparação
        Page<TopicoForum> page = new PageImpl<>(List.of());
        
        // CORREÇÃO: Usar any() para garantir que o mock responda à chamada do controller
        when(forumService.listarTopicos(any(), any(), any(PageRequest.class)))
                .thenReturn(page);

        Model model = new ExtendedModelMap();
        String view = controller.listarTopicos(null, null, 0, null, model);

        // Verificações
        assertEquals("forum/lista", view);
        assertEquals(page, model.getAttribute("topicos"));
        assertEquals(false, model.getAttribute("clienteLogado"));
        
        // CORREÇÃO CRÍTICA: assertEquals em arrays compara referência. 
        // Use assertArrayEquals para comparar o conteúdo do Enum.
        assertArrayEquals(CategoriaForum.values(), (CategoriaForum[]) model.getAttribute("categorias"));
    }

    @SuppressWarnings("null")
    @Test
    void deveVerTopicoComUsuarioAdmin() {
        // Preparação
        Cliente clienteMock = new Cliente();
        clienteMock.setId(1L);
        clienteMock.setEmail("admin@example.com");

        TopicoForum topico = new TopicoForum();
        topico.setId(1L);
        topico.setAutor(clienteMock); // Evita NullPointer em lógicas internas

        UserDetails user = User.withUsername("admin@example.com")
                .password("password")
                .authorities("ADMIN")
                .build();

        when(forumService.buscarTopicoPorId(1L)).thenReturn(topico);
        when(forumService.getRespostasLikedByUser(anyLong(), anyLong())).thenReturn(Set.of());
        when(clienteRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(clienteMock));

        Model model = new ExtendedModelMap();
        String view = controller.verTopico(1L, user, model);

        // Verificações
        assertEquals("forum/topico", view);
        assertEquals(topico, model.getAttribute("topico"));
        assertNotNull(model.getAttribute("isAdmin"));
        assertTrue((Boolean) model.getAttribute("isAdmin"));
    }

    @Test
    void deveRedirecionarParaLoginAoCriarTopicoSemUsuario() {
        NovoTopicoDTO dto = new NovoTopicoDTO();
        BindingResult result = new BeanPropertyBindingResult(dto, "novoTopico");
        Model model = new ExtendedModelMap();
        RedirectAttributes ra = new RedirectAttributesModelMap();

        // Passando null no UserDetails para simular usuário não logado
        String view = controller.criarTopico(dto, result, null, null, null, model, ra);

        assertEquals("redirect:/clientes/login", view);
    }
}