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

@Controller
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogViewController {

    private final PostBlogService postBlogService;

    @GetMapping
    public String listarBlog(@AuthenticationPrincipal UserDetails user, Model model) {
        List<PostBlog> posts = postBlogService.listarTodos();
        model.addAttribute("posts", posts);
        preencherUsuario(user, model);
        return "blog/blog";
    }

    @GetMapping("/{id}")
    public String verPost(@PathVariable Long id, @AuthenticationPrincipal UserDetails user, Model model) {
        PostBlog post = postBlogService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado"));
        model.addAttribute("post", post);
        preencherUsuario(user, model);
        return "blog/blog_post";
    }

    private void preencherUsuario(UserDetails user, Model model) {
        boolean isAdmin = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("clienteLogado", user != null);
    }
}
