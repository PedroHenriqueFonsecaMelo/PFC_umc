package umc.exs.controller.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * Proxy backend para a API do Google Books, evitando que a chave de API fique exposta no frontend.
 * O frontend consulta este controller, que repassa a requisição ao Google com a chave configurada no servidor.
 */
@RestController
@RequestMapping("/api/books")
public class GoogleBooksProxyController {

    @Value("${google.books.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Busca os dados de um livro pelo ISBN consultando a API do Google Books e retorna o JSON bruto.
     * Retorna 404 caso a requisição ao Google falhe ou o ISBN não seja encontrado.
     */
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
