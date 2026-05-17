package umc.exs.controller.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/books")
public class GoogleBooksProxyController {

    @Value("${google.books.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<String> buscarPorIsbn(@PathVariable String isbn) {
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
        if (apiKey != null && !apiKey.isBlank()) {
            url += "&key=" + apiKey;
        }
        try {
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
