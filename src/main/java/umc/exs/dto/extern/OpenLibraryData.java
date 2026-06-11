package umc.exs.dto.extern;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * Representa a resposta da API Open Library ao buscar um livro pelo ISBN.
 * Usada como fallback quando a API do Google Books não retorna resultado para o ISBN consultado.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryData {

    private String title;
    private List<Author> authors;
    private Cover cover;
    private String notes;

    /**
     * Contém o nome do autor retornado pela Open Library.
     * Campos desconhecidos da API são ignorados pelo Jackson.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        private String name;
    }

    /**
     * Contém as URLs da capa do livro em três tamanhos: small, medium e large.
     * Campos desconhecidos da API são ignorados pelo Jackson.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cover {
        private String small;
        private String medium;
        private String large;
    }
}
