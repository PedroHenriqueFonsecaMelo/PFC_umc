package umc.exs.controller.api;

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
}