package umc.exs.dto.response.admin;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.enums.StatusVenda;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendaResponse {

    private Long id;
    private String titulo;
    private String autor;
    private String isbn;
    private String idioma;
    private String resumoOficial;
    private EstadoLivro estadoAprovado;
    private Double precoAprovado;
    /** Lista completa de fotos (array deserializado de fotosUrls) */
    private List<String> fotos;
    private LocalDateTime dataAnuncio;
    private LocalDateTime dataAprovacao;
    private StatusVenda statusVenda;
    private String motivoRejeicao;

    /** Preenchidos apenas quando statusVenda == VENDIDO */
    private LocalDateTime dataVenda;
    private Double tokensRecebidos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resumo {

        private Long id;
        private String titulo;
        private String autor;
        private String isbn;
        private EstadoLivro estadoAprovado;
        private Double precoAprovado;
        private String primeiraFoto;
        private LocalDateTime dataAnuncio;
        private StatusVenda statusVenda;
        private String motivoRejeicao;
    }
}
