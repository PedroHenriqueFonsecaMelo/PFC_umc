package umc.exs.service.core.interactions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
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

@Service
@RequiredArgsConstructor
public class PostBlogService {

    private final PostBlogRepository postBlogRepository;
    private final ComentarioBlogRepository comentarioRepository;
    private static final String POST_N_ENCONTRADO = "Post não encontrado";

    public List<PostBlog> listarTodos() {
        return postBlogRepository.findAllByOrderByDataPublicacaoDesc();
    }

    public Optional<PostBlog> buscarPorId(@lombok.NonNull Long id) {
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
            throw new IllegalStateException("Apenas rascunhos podem ser submetidos para revisão.");
        }
        post.setStatus(StatusPost.EM_REVISAO);
        return postBlogRepository.save(post);
    }

    @Transactional
    public PostBlog publicar(@NonNull Long id) {
        PostBlog post = postBlogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));
        post.setStatus(StatusPost.PUBLICADO);
        post.setDataPublicacao(LocalDateTime.now());
        return postBlogRepository.save(post);
    }

    @Transactional
    public PostBlog agendar(@NonNull Long id, LocalDateTime dataAgendada) {
        PostBlog post = postBlogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));
        if (dataAgendada == null || dataAgendada.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data de agendamento deve ser futura.");
        }
        post.setStatus(StatusPost.AGENDADO);
        post.setDataPublicacaoAgendada(dataAgendada);
        return postBlogRepository.save(post);
    }

    @SuppressWarnings({"null", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
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

        return postBlogRepository.save(post);
    }

    @SuppressWarnings({"null", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
    @Transactional
    public PostBlog editarPost(Long id, String titulo, String conteudo, MultipartFile imagem) throws IOException {
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
        return postBlogRepository.save(post);
    }

    @SuppressWarnings("null")
    public void deletarPost(Long id) {
        postBlogRepository.deleteById(id);
    }

    @SuppressWarnings("null")
    @Transactional
    public int curtirPost(Long postId) {
        PostBlog post = postBlogRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));
        post.setCurtidas(post.getCurtidas() + 1);
        return postBlogRepository.save(post).getCurtidas();
    }

    @SuppressWarnings("null")
    @Transactional
    public ComentarioBlog comentar(Long postId, String autorNome, String conteudo) {
        PostBlog post = postBlogRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException(POST_N_ENCONTRADO));
        ComentarioBlog comentario = ComentarioBlog.builder()
                .post(post)
                .autorNome(autorNome)
                .conteudo(conteudo)
                .build();
        return comentarioRepository.save(comentario);
    }

    public List<ComentarioBlog> listarComentarios(Long postId) {
        return comentarioRepository.findByPostIdOrderByDataCriacaoAsc(postId);
    }
}
