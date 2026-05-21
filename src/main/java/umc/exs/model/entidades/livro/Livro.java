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
import io.github.manoelcampos.dtogen.DTO;
import umc.exs.dto.response.compras.LivroExibicaoResponse;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;

@Entity
@DTO
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

    private String idioma;

    @Column(length = 100)
    private String genero;

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

    /** Preenchido pelo admin ao rejeitar o anúncio. Visível apenas ao vendedor. */
    @Column(columnDefinition = "TEXT")
    private String motivoRejeicao;

    /** Promoção — exibe badge "PROMOÇÃO" e preço original riscado na vitrine. */
    @Builder.Default
    private Boolean emPromocao = false;

    private Double precoOriginal;

    private LocalDateTime promocaoExpira;

    // Relacionamento com as avaliações da história
    @OneToMany(mappedBy = "isbnOriginalNoAto", targetEntity = AvaliacaoLivro.class)
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;

    @ManyToOne
    @JoinColumn(name = "obra_id")
    @JsonIgnore
    private Obra obra;
}