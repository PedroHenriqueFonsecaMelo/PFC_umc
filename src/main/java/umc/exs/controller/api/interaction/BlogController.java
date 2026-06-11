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

/**
 * Controller REST para gerenciamento do blog da plataforma (posts, comentários, curtidas e agendamento).
 * Admins criam e publicam posts; clientes comentam e curtem o conteúdo.
 */
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

    /**
     * Lista todos os posts do blog; sem filtro retorna apenas os publicados.
     * Com parâmetro de status, filtra por PUBLICADO, RASCUNHO ou AGENDADO.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPosts(
            @RequestParam(required = false) StatusPost status) {
        List<PostBlog> lista = (status != null)
                ? postBlogService.listarPorStatus(status)
                : postBlogService.listarPublicados();

        // Uso de .toList() para Java 16+
        return ResponseEntity.ok(lista.stream().map(this::toMap).toList());
    }

    /** Busca um post específico do blog pelo ID; retorna 404 se não encontrado. */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> buscarPost(@PathVariable Long id) {
        return postBlogService.buscarPorId(id)
                .map(p -> ResponseEntity.ok(toMap(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Retorna todos os comentários de um post do blog com autor e data formatada. */
    @GetMapping("/{postId}/comentarios")
    public ResponseEntity<List<Map<String, Object>>> listarComentarios(@PathVariable Long postId) {
        List<Map<String, Object>> comentarios = postBlogService.listarComentarios(postId).stream()
                .<Map<String, Object>>map(c -> {
                    java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put(AUTOR_NOME, c.getAutorNome() != null ? c.getAutorNome() : "Anônimo");
                    m.put(CONTEUDO, c.getConteudo());
                    m.put("dataCriacao", c.getDataCriacao().format(FMT));
                    return m;
                })
                .toList();
        return ResponseEntity.ok(comentarios);
    }

    /** Registra ou desfaz a curtida de um cliente em um post do blog. */
    @PostMapping("/{postId}/curtir")
    public ResponseEntity<Map<String, Object>> curtirPost(
            @PathVariable Long postId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        boolean descurtir = body != null && Boolean.TRUE.equals(body.get("descurtir"));
        Map<String, Object> resultado = postBlogService.toggleCurtir(postId, descurtir);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Remove um comentário do blog; somente o autor ou admin pode deletar.
     * Retorna 403 se o usuário não tiver permissão.
     */
    @DeleteMapping("/{postId}/comentarios/{comentarioId}")
    public ResponseEntity<Map<String, String>> deletarComentario(
            @PathVariable Long postId,
            @PathVariable Long comentarioId,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        try {
            postBlogService.deletarComentario(comentarioId, user.getUsername());
            return ResponseEntity.ok(Map.of(MENSAGEM, "Comentário removido."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of(ERRO, e.getMessage()));
        }
    }

    /** Adiciona um comentário ao post do blog com o nome do cliente autenticado como autor. */
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

    /** Cria um novo post no blog com título, conteúdo e imagem opcional, atribuindo ao admin logado. */
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

    /** Atualiza título, conteúdo e imagem de um post existente no blog. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> editarPost(
            @PathVariable @NonNull Long id,
            @RequestParam("titulo") String titulo,
            @RequestParam(CONTEUDO) String conteudo,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        try {
            PostBlog post = postBlogService.editarPost(id, titulo, conteudo, imagem);
            return ResponseEntity.ok(Map.of(
                    "id", Objects.requireNonNull(post.getId()),
                    MENSAGEM, "Post atualizado com sucesso."));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(ERRO, "Falha ao salvar imagem."));
        }
    }

    /** Remove permanentemente um post do blog pelo ID. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletarPost(@PathVariable Long id) {
        postBlogService.deletarPost(id);
        return ResponseEntity.ok(Map.of(MENSAGEM, "Post removido."));
    }

    /** Submete um post para revisão antes da publicação; não é possível se o post já estiver publicado. */
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

    /** Publica imediatamente um post, tornando-o visível para todos os usuários da plataforma. */
    @PatchMapping("/{id}/publicar")
    public ResponseEntity<Map<String, Object>> publicar(@PathVariable @NonNull Long id) {
        PostBlog post = postBlogService.publicar(id);
        return ResponseEntity.ok(Map.of(
                MENSAGEM, "Post publicado.",
                STATUS, Objects.requireNonNull(post.getStatus())));
    }

    /**
     * Agenda a publicação de um post para uma data e hora futuras no formato ISO-8601.
     * O post ficará com status AGENDADO até o job de publicação automática executar.
     */
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

    /** Converte uma entidade PostBlog para mapa de dados serializável em JSON. */
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