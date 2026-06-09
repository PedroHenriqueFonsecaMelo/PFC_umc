package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import umc.exs.controller.api.interaction.ForumController;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.ForumService;

class ForumControllerUnitTest {

    private ForumService forumService;
    private ClienteRepository clienteRepository;
    private ForumController controller;

    @BeforeEach
    void setUp() {
        forumService = mock(ForumService.class);
        clienteRepository = mock(ClienteRepository.class);
        controller = new ForumController(forumService, clienteRepository);
    }

    @Test
    void listarTopicos_ComSucesso_RetornaOk() {
        TopicoForum t = mock(TopicoForum.class);
        when(t.getId()).thenReturn(1L);
        when(t.getTitulo()).thenReturn("Titulo");
        CategoriaForum cat = mock(CategoriaForum.class);
        when(cat.getDescricao()).thenReturn("Categoria");
        when(t.getCategoria()).thenReturn(cat);
        var autor = mock(umc.exs.model.entidades.usuario.Cliente.class);
        when(autor.getNome()).thenReturn("Autor");
        when(t.getAutor()).thenReturn(autor);
        LocalDateTime dt = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(t.getDataCriacao()).thenReturn(dt);
        when(t.getQtdRespostas()).thenReturn(3);
        when(t.getVisualizacoes()).thenReturn(10);
        when(t.isResolvido()).thenReturn(false);

        Page<TopicoForum> page = new PageImpl<>(List.of(t),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("dataCriacao").descending()), 1);
        when(forumService.listarTopicos(any(), any(), any())).thenReturn(page);

        ResponseEntity<Map<String, Object>> resp = controller.listarTopicos(null, null, 0, 10);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, ((List<?>) resp.getBody().get("topicos")).size());
        verify(forumService).listarTopicos(isNull(), isNull(), any());
    }

    @Test
    void deletarResposta_NaoAdmin_NaoAutor_Retorna403() {
        UserDetails user = User.withUsername("user@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        when(forumService.isAutorResposta(eq(99L), eq("user@email.com"))).thenReturn(false);

        ResponseEntity<?> resp = controller.deletarResposta(99L, user);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertTrue(((Map<?, ?>) resp.getBody()).get("erro").toString().contains("Sem permissão"));
        verify(forumService, never()).deletarResposta(anyLong());
    }

    @Test
    void deletarResposta_Admin_Retorna200() {
        UserDetails user = User.withUsername("user@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("ADMIN")))
                .build();

        ResponseEntity<?> resp = controller.deletarResposta(10L, user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(forumService).deletarResposta(10L);
    }

    @Test
    void deletarTopico_Retorna200() {
        ResponseEntity<?> resp = controller.deletarTopico(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(forumService).deletarTopico(1L);
    }

    @Test
    void curtirResposta_QuandoClienteNaoEncontrado_LançaRuntimeException() {
        UserDetails user = User.withUsername("missing@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        when(clienteRepository.findByEmail("missing@email.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.curtirResposta(1L, user));
        assertTrue(ex.getMessage().contains("Usuário não encontrado"));
        verify(forumService, never()).curtirResposta(anyLong(), anyLong());
    }
}
