package umc.exs.DTOs.livro;

import lombok.Data;
import umc.exs.model.enums.EstadoLivro;

@Data
public class LivroExibicaoDTO {
    private Long id;
    private String titulo;
    private String autor;
    private String isbn;
    private String fotoUrl;
    private String fotosUrls;
    private String descricao;
    private EstadoLivro estadoAprovado;
    private Double precoAprovado;

    public LivroExibicaoDTO(Long id, String titulo, String autor, String isbn, String fotoUrl, String fotosUrls,
            String descricao, EstadoLivro estadoAprovado, Double precoAprovado) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.fotoUrl = fotoUrl;
        this.fotosUrls = fotosUrls;
        this.descricao = descricao;
        this.estadoAprovado = estadoAprovado;
        this.precoAprovado = precoAprovado;
    }

    public LivroExibicaoDTO(String titulo, String autor, String isbn, String fotoUrl, String fotosUrls,
            String descricao,
            EstadoLivro estadoAprovado, Double precoAprovado) {
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.fotoUrl = fotoUrl;
        this.fotosUrls = fotosUrls;
        this.descricao = descricao;
        this.estadoAprovado = estadoAprovado;
        this.precoAprovado = precoAprovado;
    }

    public LivroExibicaoDTO(Long id2, String titulo2, String autor2, String isbn2, String primeiraFoto,
            EstadoLivro estadoAprovado2, Double precoAprovado2) {
        this.id = id2;
        this.titulo = titulo2;
        this.autor = autor2;
        this.isbn = isbn2;
        this.fotoUrl = primeiraFoto;
        this.estadoAprovado = estadoAprovado2;
        this.precoAprovado = precoAprovado2;
    }

}
