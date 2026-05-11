package umc.exs.controller.api.interaction;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.social.ComentarioBlog;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;
import umc.exs.model.entidades.usuario.Cliente;
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

    // Constantes para evitar duplicação de literais (Code Smell)
    private static final String AUTOR_NOME = "autorNome";
    private static final String CONTEUDO = "conteudo";
    private static final String MENSAGEM = "mensagem";
    private static final String STATUS = "status";
    private static final String DATA_AGENDADA = "dataPublicacaoAgendada";
    private static final String ERRO = "erro";

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPosts(
            @RequestParam(required = false) StatusPost status) {
        List<PostBlog> lista = (status != null)
                ? postBlogService.listarPorStatus(status)
                : postBlogService.listarPublicados();

        // Uso de .toList() para Java 16+
        return ResponseEntity.ok(lista.stream().map(this::toMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> buscarPost(@PathVariable Long id) {
        return postBlogService.buscarPorId(id)
                .map(p -> ResponseEntity.ok(toMap(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{postId}/comentarios")
    public ResponseEntity<List<Map<String, Object>>> listarComentarios(@PathVariable Long postId) {
        List<Map<String, Object>> comentarios = postBlogService.listarComentarios(postId).stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        AUTOR_NOME, c.getAutorNome() != null ? c.getAutorNome() : "Anônimo",
                        CONTEUDO, c.getConteudo(),
                        "dataCriacao", c.getDataCriacao().format(FMT)))
                .toList();
        return ResponseEntity.ok(comentarios);
    }

    @PostMapping("/{postId}/curtir")
    public ResponseEntity<Map<String, Integer>> curtirPost(@PathVariable Long postId) {
        int curtidas = postBlogService.curtirPost(postId);
        return ResponseEntity.ok(Map.of("curtidas", curtidas));
    }

    @PostMapping("/{postId}/comentarios")
    public ResponseEntity<Map<String, Object>> comentar(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {

        String conteudoInput = body.get(CONTEUDO);
        if (conteudoInput == null || conteudoInput.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(ERRO, "Conteúdo não pode ser vazio."));
        }

        // Uso de Method Reference 'Cliente::getNome'
        String autorNome = clienteRepository.findByEmail(user.getUsername())
                .map(Cliente::getNome)
                .orElse(user.getUsername());

        ComentarioBlog comentario = postBlogService.comentar(postId, autorNome, conteudoInput);

        return ResponseEntity.ok(Map.of(
                "id", Objects.requireNonNull(comentario.getId()),
                AUTOR_NOME, comentario.getAutorNome(),
                CONTEUDO, comentario.getConteudo(),
                "dataCriacao", comentario.getDataCriacao().format(FMT)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> criarPost(
            @RequestParam("titulo") String titulo,
            @RequestParam(CONTEUDO) String conteudo,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem,
            @AuthenticationPrincipal UserDetails userDetails) {

        String autorNome = adminRepository.findByEmail(userDetails.getUsername())
                .map(a -> a.getNome())
                .orElse("Administrador");

        try {
            PostBlog post = postBlogService.criarPost(titulo, conteudo, autorNome, imagem);
            return ResponseEntity.ok(Map.of(
                    "id", Objects.requireNonNull(post.getId()),
                    MENSAGEM, "Post publicado com sucesso."));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(ERRO, "Falha ao salvar imagem."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletarPost(@PathVariable Long id) {
        postBlogService.deletarPost(id);
        return ResponseEntity.ok(Map.of(MENSAGEM, "Post removido."));
    }

    @PatchMapping("/{id}/submeter")
    public ResponseEntity<Map<String, Object>> submeterParaRevisao(@PathVariable @NonNull Long id) {
        try {
            PostBlog post = postBlogService.submeterParaRevisao(id);
            return ResponseEntity.ok(Map.of(
                    MENSAGEM, "Post submetido para revisão.",
                    STATUS, post.getStatus()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(ERRO, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/publicar")
    public ResponseEntity<Map<String, Object>> publicar(@PathVariable @NonNull Long id) {
        PostBlog post = postBlogService.publicar(id);
        return ResponseEntity.ok(Map.of(
                MENSAGEM, "Post publicado.",
                STATUS, Objects.requireNonNull(post.getStatus())));
    }

    @PatchMapping("/{id}/agendar")
    public ResponseEntity<Map<String, Object>> agendar(
            @PathVariable @NonNull Long id,
            @RequestBody Map<String, String> body) {
        String dataStr = body.get(DATA_AGENDADA);
        if (dataStr == null || dataStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(ERRO, DATA_AGENDADA + " é obrigatório."));
        }
        try {
            LocalDateTime data = LocalDateTime.parse(dataStr);
            PostBlog post = postBlogService.agendar(id, data);
            return ResponseEntity.ok(Map.of(
                    MENSAGEM, "Post agendado.",
                    STATUS, Objects.requireNonNull(post.getStatus()),
                    DATA_AGENDADA, data.toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERRO, e.getMessage()));
        }
    }

    private Map<String, Object> toMap(PostBlog p) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("titulo", p.getTitulo() != null ? p.getTitulo() : "");
        map.put(CONTEUDO, p.getConteudo() != null ? p.getConteudo() : "");
        map.put("imagemUrl", p.getImagemUrl() != null ? p.getImagemUrl() : "");
        map.put(AUTOR_NOME, p.getAutorNome() != null ? p.getAutorNome() : "Administrador");
        map.put("dataPublicacao", p.getDataPublicacao().format(FMT));
        map.put("curtidas", p.getCurtidas());
        map.put(STATUS, p.getStatus() != null ? p.getStatus().name() : "PUBLICADO");
        if (p.getDataPublicacaoAgendada() != null) {
            map.put(DATA_AGENDADA, p.getDataPublicacaoAgendada().format(FMT));
        }
        return map;
    }
}