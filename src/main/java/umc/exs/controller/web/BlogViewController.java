package umc.exs.controller.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.service.core.interactions.PostBlogService;

/**
 * Renderiza as páginas do blog via Thymeleaf para listagem e visualização de posts.
 * Enriquece o model com informações do usuário autenticado para controle de permissões na view.
 */
@Controller
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogViewController {

    private final PostBlogService postBlogService;

    /**
     * Exibe a lista de todos os posts do blog na página blog.html.
     * Adiciona ao model a lista de posts e as informações do usuário autenticado.
     */
    @GetMapping
    public String listarBlog(@AuthenticationPrincipal UserDetails user, Model model) {
        List<PostBlog> posts = postBlogService.listarTodos();
        model.addAttribute("posts", posts);
        preencherUsuario(user, model);
        return "blog/blog";
    }

    /**
     * Exibe o detalhe de um post específico pelo ID na página blog_post.html.
     * Lança erro 404 caso o post não seja encontrado.
     */
    @GetMapping("/{id}")
    public String verPost(@PathVariable Long id, @AuthenticationPrincipal UserDetails user, Model model) {
        PostBlog post = postBlogService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado"));
        model.addAttribute("post", post);
        preencherUsuario(user, model);
        return "blog/blog_post";
    }

    /**
     * Adiciona ao model os atributos "clienteLogado" e "isAdmin" para controle de permissões na view.
     * Utilizado por todos os métodos do controller antes de renderizar o template.
     */
    private void preencherUsuario(UserDetails user, Model model) {
        boolean isAdmin = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("clienteLogado", user != null);
    }
}
