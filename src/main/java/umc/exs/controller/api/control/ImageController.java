package umc.exs.controller.api.control;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImageController {

    /**
     * Serve imagens de livros do uploads.
     * Retorna FileSystemResource ou default no-image se não existe.
     * Endpoint direto /uploads/livros/filename.
     * Usado por frontend vitrine.
     */
    @SuppressWarnings("null")
    @GetMapping("/uploads/livros/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {

        Path path = Paths.get("uploads/livros/" + filename);

        Resource file = new FileSystemResource(path);

        if (file.exists()) {
            return ResponseEntity.ok(file);
        } else {

            return ResponseEntity.ok(new ClassPathResource("static/images/no-image.png"));
        }
    }

    /**
     * Serve fotos de perfil dos clientes.
     * Endpoint /uploads/clientes/filename.
     */
    @SuppressWarnings("null")
    @GetMapping("/uploads/clientes/{filename:.+}")
    public ResponseEntity<Resource> serveClienteImage(@PathVariable String filename) {
        Path path = Paths.get("uploads/clientes/" + filename);
        Resource file = new FileSystemResource(path);
        if (file.exists()) {
            return ResponseEntity.ok(file);
        } else {
            return ResponseEntity.ok(new ClassPathResource("static/images/no-image.png"));
        }
    }

    /**
     * Serve imagens de posts do blog.
     * Endpoint /uploads/blog/filename.
     */
    @SuppressWarnings("null")
    @GetMapping("/uploads/blog/{filename:.+}")
    public ResponseEntity<Resource> serveBlogImage(@PathVariable String filename) {

        Path path = Paths.get("uploads/blog/" + filename);
        Resource file = new FileSystemResource(path);

        if (file.exists()) {
            return ResponseEntity.ok(file);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller para servir imagens de livros estáticas.
 * Endpoint /uploads/livros/{filename} retorna arquivo ou no-image.png.
 * Usa FileSystemResource/ClassPathResource.
 * Essencial para vitrine frontend.
 */
