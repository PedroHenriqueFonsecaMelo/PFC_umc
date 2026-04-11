package umc.exs.model.entidades.livro;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.DTOs.livro.LivroExibicaoDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Livro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String autor;

    private String isbn;

    @Column(name = "fotos_urls", columnDefinition = "TEXT")
    @Builder.Default
    private String fotosUrls = "[]";

    @Column(columnDefinition = "TEXT")
    private String resumoOficial;

    @ManyToOne(optional = true)
    @JoinColumn(name = "lote_id")
    @JsonIgnore
    private Lote lote;

    @ManyToOne(optional = true)
    @JoinColumn(name = "vendedor_id")
    @JsonIgnore
    private Cliente vendedor;

    private LocalDateTime dataAnuncio;

    @Builder.Default
    private Boolean aprovado = false;

    private Double precoAprovado;

    @Enumerated(EnumType.STRING)
    private EstadoLivro estadoAprovado;

    private LocalDateTime dataAprovacao;

    private Long adminAprovadorId;

    // Relacionamento com as avaliações da história
    @OneToMany(mappedBy = "isbnOriginalNoAto", targetEntity = AvaliacaoLivro.class)
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;

    @ManyToOne
    @JoinColumn(name = "obra_id")
    @JsonIgnore
    private Obra obra;

    public LivroExibicaoDTO paraDTO() {
        String primeiraFoto = "";
        try {
            if (fotosUrls != null && fotosUrls.contains("\"")) {
                primeiraFoto = fotosUrls.split("\"")[1];
            }
        } catch (Exception e) {
            primeiraFoto = "";
        }
        return new LivroExibicaoDTO(id, titulo, autor, isbn, primeiraFoto, estadoAprovado, precoAprovado);
    }
}