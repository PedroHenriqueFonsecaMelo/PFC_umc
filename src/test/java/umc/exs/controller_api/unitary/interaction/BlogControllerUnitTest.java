package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import umc.exs.controller.api.interaction.BlogController;
import umc.exs.model.entidades.social.ComentarioBlog;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.PostBlogService;

class BlogControllerUnitTest {

    private PostBlogService postBlogService;
    private AdminRepository adminRepository;
    private ClienteRepository clienteRepository;
    private BlogController controller;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @BeforeEach
    void setUp() {
        postBlogService = mock(PostBlogService.class);
        adminRepository = mock(AdminRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        controller = new BlogController(postBlogService, adminRepository, clienteRepository);
    }

    @Test
    void listarPosts_SemStatus_UsaListarPublicados() {
        PostBlog p = mock(PostBlog.class);
        when(postBlogService.listarPublicados()).thenReturn(List.of(p));
        when(p.getId()).thenReturn(1L);
        when(p.getDataPublicacao()).thenReturn(LocalDateTime.now());

        ResponseEntity<List<Map<String, Object>>> resp = controller.listarPosts(null);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        verify(postBlogService).listarPublicados();
        verify(postBlogService, never()).listarPorStatus(any());
    }

    @Test
    void curtirPost_SemAuth_Retorna401() {
        ResponseEntity<Map<String, Object>> resp = controller.curtirPost(1L, Map.of(), null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void deletarComentario_SemException_Retorna200() {
        UserDetails user = User.withUsername("user@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        ResponseEntity<Map<String, String>> resp = controller.deletarComentario(1L, 2L, user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(postBlogService).deletarComentario(eq(2L), eq(user.getUsername()));
    }

    @Test
    void deletarComentario_IllegalState_Retorna403() {
        UserDetails user = User.withUsername("user@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        doThrow(new IllegalStateException("sem permissão"))
                .when(postBlogService).deletarComentario(eq(2L), eq(user.getUsername()));

        ResponseEntity<Map<String, String>> resp = controller.deletarComentario(1L, 2L, user);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("sem permissão", resp.getBody().get("erro"));
    }

    @Test
    void comentar_ConteudoVazio_Retorna400() {
        UserDetails user = User.withUsername("user@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        ResponseEntity<Map<String, Object>> resp = controller.comentar(
                1L,
                Map.of("conteudo", "   "),
                user);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().get("erro").toString().contains("Conteúdo"));
        verifyNoInteractions(postBlogService);
    }

    @Test
    void comentar_ComSucesso_UsaNomeDoCliente() {
        UserDetails user = User.withUsername("user@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        // Cliente
        var cliente = mock(umc.exs.model.entidades.usuario.Cliente.class);
        when(cliente.getNome()).thenReturn("João");
        when(clienteRepository.findByEmail("user@email.com")).thenReturn(Optional.of(cliente));

        ComentarioBlog c = mock(ComentarioBlog.class);
        when(c.getId()).thenReturn(9L);
        when(c.getAutorNome()).thenReturn("João");
        when(c.getConteudo()).thenReturn("texto");
        when(c.getDataCriacao()).thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));
        when(postBlogService.comentar(eq(1L), eq("João"), eq("texto"))).thenReturn(c);

        ResponseEntity<Map<String, Object>> resp = controller.comentar(
                1L,
                Map.of("conteudo", "texto"),
                user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(9L, resp.getBody().get("id"));
        assertEquals("João", resp.getBody().get("autorNome"));
        assertEquals("texto", resp.getBody().get("conteudo"));
        assertEquals(FMT.format(c.getDataCriacao()), resp.getBody().get("dataCriacao"));
    }

    @Test
    void criarPost_ComImagemIOException_Retorna500() throws Exception {
        UserDetails user = User.withUsername("admin@email.com")
                .password("pass")
                .authorities(List.of(new SimpleGrantedAuthority("ADMIN")))
                .build();

        MultipartFile file = mock(MultipartFile.class);
        when(adminRepository.findByEmail("admin@email.com"))
                .thenReturn(Optional.empty());

        doThrow(new IOException("falha"))
                .when(postBlogService).criarPost(anyString(), anyString(), anyString(), eq(file));

        ResponseEntity<Map<String, Object>> resp = controller.criarPost(
                "título",
                "conteudo",
                file,
                user);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("Falha ao salvar imagem.", resp.getBody().get("erro"));
    }

    @Test
    void editarPost_Sucesso_Retorna200() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        PostBlog post = mock(PostBlog.class);
        when(post.getId()).thenReturn(50L);
        when(postBlogService.editarPost(eq(10L), eq("titulo"), eq("conteudo"), eq(file))).thenReturn(post);

        ResponseEntity<Map<String, Object>> resp = controller.editarPost(
                10L,
                "titulo",
                "conteudo",
                file);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(50L, resp.getBody().get("id"));
        assertEquals("Post atualizado com sucesso.", resp.getBody().get("mensagem"));
    }

    @Test
    void agendar_DataObrigatoriaVazia_Retorna400() {
        ResponseEntity<Map<String, Object>> resp = controller.agendar(1L, Map.of());
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().get("erro").toString().contains("dataPublicacaoAgendada"));
    }

    @Test
    void publicar_Sucesso_Retorna200() {
        PostBlog post = mock(PostBlog.class);
        when(post.getStatus()).thenReturn(StatusPost.PUBLICADO);
        when(postBlogService.publicar(1L)).thenReturn(post);

        ResponseEntity<Map<String, Object>> resp = controller.publicar(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Post publicado.", resp.getBody().get("mensagem"));
        assertEquals(StatusPost.PUBLICADO, resp.getBody().get("status"));
    }
}
