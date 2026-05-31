package umc.exs.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ArquivosService {

    public static String salvarFoto(MultipartFile arquivo, String subPasta) {
        try {
            String nomeArquivo = UUID.randomUUID() + "_" + arquivo.getOriginalFilename();
            Path destino = Paths.get("uploads/" + subPasta + "/" + nomeArquivo);
            Files.createDirectories(destino.getParent());
            Files.copy(arquivo.getInputStream(), destino);
            return "/uploads/" + subPasta + "/" + nomeArquivo;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao armazenar arquivo.", e);
        }
    }

    public static String salvarArquivoFisico(MultipartFile foto) {
        try {
            String nomeArquivo = UUID.randomUUID() + "_" + foto.getOriginalFilename();
            Path destino = Paths.get("uploads/clientes/" + nomeArquivo);
            Files.createDirectories(destino.getParent());
            Files.copy(foto.getInputStream(), destino);
            return "/uploads/clientes/" + nomeArquivo;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao armazenar arquivo.", e);
        }
    }

    public static String salvarFotoLivro(MultipartFile foto, String url_upload) {
        String nome = UUID.randomUUID() + "_" + sanitizarNome(foto.getOriginalFilename());
        Path caminho = Paths.get(url_upload + nome);
        try {
            Files.createDirectories(caminho.getParent());
            Files.copy(foto.getInputStream(), caminho);
            return "/" + url_upload + nome;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar foto: " + e.getMessage());
        }
    }

    private static String sanitizarNome(String nome) {
        if (nome == null || nome.isBlank())
            return "imagem.jpg";
        String base = Paths.get(nome).getFileName().toString();
        String s = base.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        return s.isBlank() ? "imagem.jpg" : s;
    }
}
