package umc.exs.service.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;
import umc.exs.converter.GeneroMapper;
import umc.exs.dto.extern.GoogleBookData;
import umc.exs.dto.extern.OpenLibraryData;
import umc.exs.model.entidades.livro.Livro;

@Slf4j
@Service
public class ExternApi {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.books.api-key:}")
    private String googleBooksApiKey;

    /**
     * Busca na Google Books API. Retorna null (sem lançar exceção) se o serviço
     * estiver indisponível ou ocorrer qualquer erro de rede/HTTP.
     */
    @Async
    public CompletableFuture<GoogleBookData> buscarPorIsbnAsync(String isbn) {
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:"
                + isbn
                + (googleBooksApiKey != null && !googleBooksApiKey.isBlank()
                        ? "&key=" + googleBooksApiKey
                        : "");
        try {
            GoogleBookData response = restTemplate.getForObject(url, GoogleBookData.class);
            return CompletableFuture.completedFuture(response);
        } catch (RestClientException e) {
            log.warn("Google Books API indisponível para ISBN {}: {}", isbn, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Fallback: busca na OpenLibrary API.
     * Retorna Optional vazio se o livro não for encontrado ou se a API falhar.
     */
    public String buscarGeneroPorIsbn(String isbn) {
        if (isbn == null || isbn.isBlank())
            return null;
        try {
            String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:"
                    + isbn.replaceAll("[^0-9X]", "")
                    + (googleBooksApiKey != null && !googleBooksApiKey.isBlank()
                            ? "&key=" + googleBooksApiKey
                            : "");
            GoogleBookData response = restTemplate.getForObject(url, GoogleBookData.class);
            if (response == null || response.getItems() == null || response.getItems().isEmpty())
                return null;
            var categories = response.getItems().get(0).getVolumeInfo().getCategories();
            if (categories == null || categories.isEmpty())
                return null;
            return GeneroMapper.traduzir(categories.get(0));
        } catch (Exception e) {
            return null;
        }
    }

    public Optional<Livro> buscarPorIsbnOpenLibrary(String isbn) {
        String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
        try {
            Map<String, OpenLibraryData> result = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, OpenLibraryData>>() {
                    }).getBody();

            if (result == null || result.isEmpty()) {
                return Optional.empty();
            }

            OpenLibraryData data = result.values().iterator().next();
            if (data == null || data.getTitle() == null) {
                return Optional.empty();
            }

            String autor = (data.getAuthors() != null && !data.getAuthors().isEmpty())
                    ? data.getAuthors().get(0).getName()
                    : "Autor Desconhecido";

            String capa = "";
            if (data.getCover() != null) {
                capa = data.getCover().getMedium() != null
                        ? data.getCover().getMedium()
                        : (data.getCover().getLarge() != null ? data.getCover().getLarge() : "");
            }

            Livro dto = new Livro();
            dto.setTitulo(data.getTitle());
            dto.setAutor(autor);
            dto.setIsbn(isbn);
            dto.setResumoOficial(data.getNotes());
            // Reutiliza o campo fotosUrls para transportar a capa ao frontend
            if (!capa.isEmpty()) {
                dto.setFotosUrls(capa);
            }

            return Optional.of(dto);

        } catch (RestClientException e) {
            log.warn("OpenLibrary API indisponível para ISBN {}: {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }
}
