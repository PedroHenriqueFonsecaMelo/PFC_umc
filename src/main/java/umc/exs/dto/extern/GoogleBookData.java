package umc.exs.dto.extern;

import java.util.List;

import lombok.Data;

/**
 * Representa a resposta da API do Google Books ao buscar um livro pelo ISBN.
 * Contém a lista de volumes encontrados, cada um com seus metadados e imagens.
 */
@Data
public class GoogleBookData {

    private List<Item> items;

    /**
     * Representa cada volume retornado pela API do Google Books.
     * Encapsula as informações detalhadas do livro através do campo volumeInfo.
     */
    @Data
    public static class Item {
        private VolumeInfo volumeInfo;
    }

    /**
     * Contém os metadados do livro: título, autores, idioma, descrição, categorias e capa.
     * Mapeado diretamente dos campos retornados pela API do Google Books.
     */
    @Data
    public static class VolumeInfo {
        private String title;
        private List<String> authors;
        private String language;
        private String description;
        private ImageLinks imageLinks;
        private List<String> categories;
        private Integer pageCount;
        private String publishedDate;
        private String publisher;
    }

    /**
     * Contém as URLs das imagens de capa do livro em tamanhos diferentes.
     * thumbnail é a versão padrão e smallThumbnail é a versão reduzida.
     */
    @Data
    public static class ImageLinks {
        private String thumbnail;
        private String smallThumbnail;

    }

}
