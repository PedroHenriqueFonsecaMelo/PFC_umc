package umc.exs.service.core.bussiness;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import umc.exs.dtos.livro.GoogleBookResponse;

@Service
public class GoogleBooksService {

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleBookResponse buscarPorIsbn(String isbn) {

        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

        return restTemplate.getForObject(url, GoogleBookResponse.class);
    }
}