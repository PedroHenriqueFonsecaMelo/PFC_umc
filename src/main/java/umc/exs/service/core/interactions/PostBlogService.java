package umc.exs.service.core.interactions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.social.ComentarioBlog;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.model.enums.StatusPost;
import umc.exs.repository.negocios.ComentarioBlogRepository;
import umc.exs.repository.negocios.PostBlogRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;

@Service
@RequiredArgsConstructor
public class PostBlogService {

    private final PostBlogRepository postBlogRepository;
    private final ComentarioBlogRepository comentarioRepository;
    private final ClienteRepository clienteRepository;
    private final AppLogger appLogger;

    private static final String POST_N_ENCONTRADO = "Post não encontrado";

    public List<PostBlog> listarTodos() {
        return postBlogRepository.findAllByOrderByDataPublicacaoDesc();
    }

    public Optional<PostBlog> buscarPorId(@NonNull Long id) {
        return postBlogRepository.findById(id);
    }

    public List<PostBlog> listarPublicados() {
        return postBlogRepository.findByStatusOrderByDataPublicacaoDesc(StatusPost.PUBLICADO);
    }

    public List<PostBlog> listarPorStatus(StatusPost status) {
        return postBlogRepository.findByStatusOrderByDataPublicacaoDesc(status);
    }

    @Transactional
    public PostBlog submeterParaRevisao(@NonNull Long id) {

        PostBlog post = postBlogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));

        if (post.getStatus() != StatusPost.RASCUNHO) {
            throw new IllegalStateException("Apenas rascunhos");
        }

        post.setStatus(StatusPost.EM_REVISAO);

        PostBlog salvo = postBlogRepository.save(post);

        appLogger.info(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "POST_REVISAO id=" + id);

        return salvo;
    }

    @Transactional
    public PostBlog publicar(@NonNull Long id) {

        PostBlog post = postBlogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));

        post.setStatus(StatusPost.PUBLICADO);
        post.setDataPublicacao(LocalDateTime.now());

        PostBlog salvo = postBlogRepository.save(post);

        appLogger.success(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "POST_PUBLICADO id=" + id);

        return salvo;
    }

    @Transactional
    public PostBlog agendar(@NonNull Long id, LocalDateTime dataAgendada) {

        PostBlog post = postBlogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));

        if (dataAgendada == null || dataAgendada.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data inválida");
        }

        post.setStatus(StatusPost.AGENDADO);
        post.setDataPublicacaoAgendada(dataAgendada);

        PostBlog salvo = postBlogRepository.save(post);

        appLogger.info(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "POST_AGENDADO id=" + id);

        return salvo;
    }

    public PostBlog criarPost(String titulo, String conteudo, String autorNome, MultipartFile imagem)
            throws IOException {

        String imagemUrl = null;

        if (imagem != null && !imagem.isEmpty()) {

            String ext = "";
            String original = imagem.getOriginalFilename();

            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }

            String nomeFoto = UUID.randomUUID() + ext;

            Path caminho = Paths.get("uploads/blog/" + nomeFoto);

            Files.createDirectories(caminho.getParent());
            Files.copy(imagem.getInputStream(), caminho);

            imagemUrl = "/uploads/blog/" + nomeFoto;
        }

        PostBlog post = PostBlog.builder()
                .titulo(titulo)
                .conteudo(conteudo)
                .autorNome(autorNome)
                .imagemUrl(imagemUrl)
                .status(StatusPost.PUBLICADO)
                .dataPublicacao(LocalDateTime.now())
                .build();

        PostBlog salvo = postBlogRepository.save(post);

        appLogger.success(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "POST_CRIADO");

        return salvo;
    }

    @Transactional
    public PostBlog editarPost(Long id, String titulo, String conteudo, MultipartFile imagem)
            throws IOException {

        PostBlog post = postBlogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));

        if (titulo != null && !titulo.isBlank()) post.setTitulo(titulo);
        if (conteudo != null && !conteudo.isBlank()) post.setConteudo(conteudo);

        if (imagem != null && !imagem.isEmpty()) {

            String ext = "";
            String original = imagem.getOriginalFilename();

            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }

            String nomeFoto = UUID.randomUUID() + ext;

            Path caminho = Paths.get("uploads/blog/" + nomeFoto);

            Files.createDirectories(caminho.getParent());
            Files.copy(imagem.getInputStream(), caminho);

            post.setImagemUrl("/uploads/blog/" + nomeFoto);
        }

        PostBlog salvo = postBlogRepository.save(post);

        appLogger.info(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "POST_EDITADO id=" + id);

        return salvo;
    }

    public void deletarPost(Long id) {

        postBlogRepository.deleteById(id);

        appLogger.error(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "POST_DELETADO id=" + id);
    }

    @Transactional
    public Map<String, Object> toggleCurtir(Long postId, boolean descurtir) {

        PostBlog post = postBlogRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));

        if (descurtir) {
            post.setCurtidas(Math.max(0, post.getCurtidas() - 1));
        } else {
            post.setCurtidas(post.getCurtidas() + 1);
        }

        int total = postBlogRepository.save(post).getCurtidas();

        appLogger.info(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "POST_CURTIDA id=" + postId + " descurtir=" + descurtir);

        return Map.of("curtidas", total, "curtiu", !descurtir);
    }

    @Transactional
    public void deletarComentario(Long comentarioId, String emailUsuario) {

        ComentarioBlog comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado"));

        String nomeAutor = clienteRepository.findByEmail(emailUsuario)
                .map(c -> c.getNome())
                .orElse(null);

        if (nomeAutor == null || !nomeAutor.equals(comentario.getAutorNome())) {
            throw new IllegalStateException("Sem permissão");
        }

        comentarioRepository.deleteById(comentarioId);

        appLogger.error(
                AcaoAuditoria.GENERICO,
                null,
                emailUsuario,
                "COMENTARIO_DELETADO id=" + comentarioId);
    }

    @Transactional
    public ComentarioBlog comentar(Long postId, String autorNome, String conteudo) {

        PostBlog post = postBlogRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));

        ComentarioBlog comentario = ComentarioBlog.builder()
                .post(post)
                .autorNome(autorNome)
                .conteudo(conteudo)
                .build();

        ComentarioBlog salvo = comentarioRepository.save(comentario);

        appLogger.success(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "COMENTARIO_CRIADO postId=" + postId);

        return salvo;
    }

    public List<ComentarioBlog> listarComentarios(Long postId) {
        return comentarioRepository.findByPostIdOrderByDataCriacaoAsc(postId);
    }
}