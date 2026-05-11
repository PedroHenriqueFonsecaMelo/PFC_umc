package umc.exs.service.core.bussiness;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import umc.exs.dtos.livro.GoogleBookResponse;

@Service
public class GoogleBooksService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public CompletableFuture<GoogleBookResponse> buscarPorIsbnAsync(String isbn) {
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
        GoogleBookResponse response = restTemplate.getForObject(url, GoogleBookResponse.class);
        return CompletableFuture.completedFuture(response);
    }
}