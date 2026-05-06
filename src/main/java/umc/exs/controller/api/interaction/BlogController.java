package umc.exs.controller.api.interaction;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import umc.exs.model.enums.StatusPost;

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
    public ResponseEntity<List<Map<String, Object>>> listarPosts(
            @RequestParam(required = false) StatusPost status) {
        List<PostBlog> lista = (status != null)
                ? postBlogService.listarPorStatus(status)
                : postBlogService.listarPublicados();
        return ResponseEntity.ok(lista.stream().map(this::toMap).collect(Collectors.toList()));
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

    @PatchMapping("/{id}/submeter")
    public ResponseEntity<?> submeterParaRevisao(@PathVariable Long id) {
        try {
            PostBlog post = postBlogService.submeterParaRevisao(id);
            return ResponseEntity.ok(Map.of("mensagem", "Post submetido para revisão.", "status", post.getStatus()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/publicar")
    public ResponseEntity<?> publicar(@PathVariable Long id) {
        PostBlog post = postBlogService.publicar(id);
        return ResponseEntity.ok(Map.of("mensagem", "Post publicado.", "status", post.getStatus()));
    }

    @PatchMapping("/{id}/agendar")
    public ResponseEntity<?> agendar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String dataStr = body.get("dataPublicacaoAgendada");
        if (dataStr == null || dataStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "dataPublicacaoAgendada é obrigatório."));
        }
        try {
            LocalDateTime data = LocalDateTime.parse(dataStr);
            PostBlog post = postBlogService.agendar(id, data);
            return ResponseEntity.ok(Map.of("mensagem", "Post agendado.", "status", post.getStatus(),
                    "dataPublicacaoAgendada", data.toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(PostBlog p) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("titulo", p.getTitulo() != null ? p.getTitulo() : "");
        map.put("conteudo", p.getConteudo() != null ? p.getConteudo() : "");
        map.put("imagemUrl", p.getImagemUrl() != null ? p.getImagemUrl() : "");
        map.put("autorNome", p.getAutorNome() != null ? p.getAutorNome() : "Administrador");
        map.put("dataPublicacao", p.getDataPublicacao().format(FMT));
        map.put("curtidas", p.getCurtidas());
        map.put("status", p.getStatus() != null ? p.getStatus().name() : "PUBLICADO");
        if (p.getDataPublicacaoAgendada() != null) {
            map.put("dataPublicacaoAgendada", p.getDataPublicacaoAgendada().format(FMT));
        }
        return map;
    }
}
