package umc.exs.service.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ArquivosServiceTest {

    @Test
    void salvarFoto_deveCriarArquivoERetornarCaminho() throws Exception {
        MultipartFile arquivo = new MockMultipartFile(
                "file", "teste.txt", "text/plain", "conteudo".getBytes());

        String caminho = ArquivosService.salvarFoto(arquivo, "clientes");

        assertNotNull(caminho);
        assertTrue(caminho.contains("/uploads/clientes/") || caminho.contains("/uploads/clientes"));
        String cleaned = caminho.replaceFirst("^/", "");
        Path arquivoCriado = Path.of(cleaned);
        assertTrue(Files.exists(arquivoCriado));
        Files.deleteIfExists(arquivoCriado);
    }
}
