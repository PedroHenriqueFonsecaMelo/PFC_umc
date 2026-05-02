package umc.exs.service.core.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ArquivosService {

    public String salvarFoto(MultipartFile arquivo, String subPasta) {
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
}
