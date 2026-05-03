package umc.exs.service.core.interactions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.social.ComentarioBlog;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.repository.negocios.ComentarioBlogRepository;
import umc.exs.repository.negocios.PostBlogRepository;

@Service
@RequiredArgsConstructor
public class PostBlogService {

    private final PostBlogRepository postBlogRepository;
    private final ComentarioBlogRepository comentarioRepository;

    public List<PostBlog> listarTodos() {
        return postBlogRepository.findAllByOrderByDataPublicacaoDesc();
    }

    public Optional<PostBlog> buscarPorId(Long id) {
        return postBlogRepository.findById(id);
    }

    @SuppressWarnings("null")
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
                .build();

        return postBlogRepository.save(post);
    }

    @SuppressWarnings("null")
    public void deletarPost(Long id) {
        postBlogRepository.deleteById(id);
    }

    @Transactional
    public int curtirPost(Long postId) {
        PostBlog post = postBlogRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado"));
        post.setCurtidas(post.getCurtidas() + 1);
        return postBlogRepository.save(post).getCurtidas();
    }

    @Transactional
    public ComentarioBlog comentar(Long postId, String autorNome, String conteudo) {
        PostBlog post = postBlogRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado"));
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
