package umc.exs.dto.response.admin;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.enums.StatusVenda;

/**
 * DTO de resposta com os detalhes completos de um livro anunciado pelo vendedor.
 * Inclui status da venda e dados financeiros (data e tokens recebidos) quando o livro foi vendido.
 */
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

    /**
     * DTO resumido de um anúncio exibido na listagem de Minhas Vendas do vendedor.
     * Contém apenas os campos essenciais para a listagem, sem dados financeiros detalhados.
     */
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
