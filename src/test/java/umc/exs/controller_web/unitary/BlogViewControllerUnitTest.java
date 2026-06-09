package umc.exs.controller_web.unitary;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import umc.exs.controller.web.BlogViewController;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.service.core.interactions.PostBlogService;

class BlogViewControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private PostBlogService postBlogService;

    @InjectMocks
    private BlogViewController blogViewController;

    // Variável para armazenar o usuário que queremos simular em cada teste
    private UserDetails usuarioSimulado;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Criamos um resolvedor customizado simples para injetar 'usuarioSimulado' sempre que houver UserDetails no parâmetro
        HandlerMethodArgumentResolver mockAuthResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return usuarioSimulado; // Retorna o usuário definido no teste atual
            }
        };

        this.mockMvc = MockMvcBuilders.standaloneSetup(blogViewController)
                .setCustomArgumentResolvers(mockAuthResolver)
                .build();
    }

    @Test
    void listarBlog_DeveRenderizarViewComPosts() throws Exception {
        PostBlog p = mock(PostBlog.class);
        when(postBlogService.listarTodos()).thenReturn(List.of(p));

        // Define o usuário para este teste (Comum/USER)
        this.usuarioSimulado = new User("u@e.com", "x", List.of(new SimpleGrantedAuthority("USER")));

        mockMvc.perform(get("/blog"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog/blog"))
                .andExpect(model().attribute("posts", List.of(p)))
                .andExpect(model().attribute("isAdmin", false))
                .andExpect(model().attribute("clienteLogado", true));
    }

    @Test
    void verPost_DeveRenderizarViewQuandoExistente() throws Exception {
        Long id = 10L;
        PostBlog post = mock(PostBlog.class);
        when(postBlogService.buscarPorId(id)).thenReturn(Optional.of(post));

        // Define o usuário para este teste (ADMIN) -> exatamente "ADMIN" como seu anyMatch busca
        this.usuarioSimulado = new User("admin@e.com", "x", List.of(new SimpleGrantedAuthority("ADMIN")));

        mockMvc.perform(get("/blog/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("blog/blog_post"))
                .andExpect(model().attribute("post", post))
                .andExpect(model().attribute("isAdmin", true))
                .andExpect(model().attribute("clienteLogado", true));
    }

    @Test
    void verPost_DeveRetornar404QuandoNaoExistente() throws Exception {
        Long id = 99L;
        when(postBlogService.buscarPorId(id)).thenReturn(Optional.empty());
        
        // Simula usuário deslogado (null)
        this.usuarioSimulado = null; 

        mockMvc.perform(get("/blog/" + id))
                .andExpect(status().isNotFound());
    }
}