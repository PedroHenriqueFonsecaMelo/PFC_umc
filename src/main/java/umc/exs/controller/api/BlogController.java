package umc.exs.controller.api;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.service.core.PostBlogService;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final PostBlogService postBlogService;
    private final AdminRepository adminRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPosts() {
        List<Map<String, Object>> posts = postBlogService.listarTodos().stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "titulo", p.getTitulo(),
                        "conteudo", p.getConteudo(),
                        "imagemUrl", p.getImagemUrl() != null ? p.getImagemUrl() : "",
                        "autorNome", p.getAutorNome() != null ? p.getAutorNome() : "Administrador",
                        "dataPublicacao", p.getDataPublicacao().format(FMT)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(posts);
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
}
