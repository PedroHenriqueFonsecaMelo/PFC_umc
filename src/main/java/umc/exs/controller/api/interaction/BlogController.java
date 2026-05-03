package umc.exs.controller.api.interaction;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.social.ComentarioBlog;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.PostBlogService;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final PostBlogService postBlogService;
    private final AdminRepository adminRepository;
    private final ClienteRepository clienteRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPosts() {
        List<Map<String, Object>> posts = postBlogService.listarTodos().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPost(@PathVariable Long id) {
        return postBlogService.buscarPorId(id)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(toMap(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{postId}/comentarios")
    public ResponseEntity<List<Map<String, Object>>> listarComentarios(@PathVariable Long postId) {
        List<Map<String, Object>> comentarios = postBlogService.listarComentarios(postId).stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "autorNome", c.getAutorNome() != null ? c.getAutorNome() : "Anônimo",
                        "conteudo", c.getConteudo(),
                        "dataCriacao", c.getDataCriacao().format(FMT)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(comentarios);
    }

    @PostMapping("/{postId}/curtir")
    public ResponseEntity<?> curtirPost(@PathVariable Long postId) {
        int curtidas = postBlogService.curtirPost(postId);
        return ResponseEntity.ok(Map.of("curtidas", curtidas));
    }

    @PostMapping("/{postId}/comentarios")
    public ResponseEntity<?> comentar(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {

        String conteudo = body.get("conteudo");
        if (conteudo == null || conteudo.isBlank())
            return ResponseEntity.badRequest().body(Map.of("erro", "Conteúdo não pode ser vazio."));

        String autorNome = clienteRepository.findByEmail(user.getUsername())
                .map(c -> c.getNome())
                .orElse(user.getUsername());

        ComentarioBlog comentario = postBlogService.comentar(postId, autorNome, conteudo);
        return ResponseEntity.ok(Map.<String, Object>of(
                "id", comentario.getId(),
                "autorNome", comentario.getAutorNome(),
                "conteudo", comentario.getConteudo(),
                "dataCriacao", comentario.getDataCriacao().format(FMT)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarPost(
            @RequestParam("titulo") String titulo,
            @RequestParam("conteudo") String conteudo,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem,
            @AuthenticationPrincipal UserDetails userDetails) {

        String autorNome = adminRepository.findByEmail(userDetails.getUsername())
                .map(a -> a.getNome())
                .orElse("Administrador");

        try {
            PostBlog post = postBlogService.criarPost(titulo, conteudo, autorNome, imagem);
            return ResponseEntity.ok(Map.of("id", post.getId(), "mensagem", "Post publicado com sucesso."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Falha ao salvar imagem."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarPost(@PathVariable Long id) {
        postBlogService.deletarPost(id);
        return ResponseEntity.ok(Map.of("mensagem", "Post removido."));
    }

    private Map<String, Object> toMap(PostBlog p) {
        return Map.of(
                "id", p.getId(),
                "titulo", p.getTitulo() != null ? p.getTitulo() : "",
                "conteudo", p.getConteudo() != null ? p.getConteudo() : "",
                "imagemUrl", p.getImagemUrl() != null ? p.getImagemUrl() : "",
                "autorNome", p.getAutorNome() != null ? p.getAutorNome() : "Administrador",
                "dataPublicacao", p.getDataPublicacao().format(FMT),
                "curtidas", p.getCurtidas());
    }
}
