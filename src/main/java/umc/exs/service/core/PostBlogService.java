package umc.exs.service.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.social.PostBlog;
import umc.exs.repository.negocios.PostBlogRepository;

@Service
@RequiredArgsConstructor
public class PostBlogService {

    private final PostBlogRepository postBlogRepository;

    public List<PostBlog> listarTodos() {
        return postBlogRepository.findAllByOrderByDataPublicacaoDesc();
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
}
