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

import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;

/**
 * Representa um exemplar físico de livro anunciado por um vendedor na plataforma.
 * Passa por fluxo de aprovação do admin antes de aparecer na vitrine pública.
 */
@Entity

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Livro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dados bibliográficos básicos do exemplar
    private String titulo;

    private String autor;

    private String isbn;

    private String idioma;

    @Column(length = 100)
    private String genero;

    // URLs das fotos armazenadas como JSON; valor padrão é lista vazia
    @Column(name = "fotos_urls", columnDefinition = "TEXT")
    @Builder.Default
    private String fotosUrls = "[]";

    @Column(columnDefinition = "TEXT")
    private String resumoOficial;

    // Lote ao qual este exemplar pertence (opcional)
    @ManyToOne(optional = true)
    @JoinColumn(name = "lote_id")
    @JsonIgnore
    private Lote lote;

    // Cliente que está vendendo este exemplar
    @ManyToOne(optional = true)
    @JoinColumn(name = "vendedor_id")
    @JsonIgnore
    private Cliente vendedor;

    private LocalDateTime dataAnuncio;

    // Controle de aprovação: false enquanto aguarda revisão do admin
    @Builder.Default
    private Boolean aprovado = false;

    // Preço e estado (Novo, Usado, etc.) definidos pelo admin na aprovação
    private Double precoAprovado;

    @Enumerated(EnumType.STRING)
    private EstadoLivro estadoAprovado;

    private LocalDateTime dataAprovacao;

    // ID do admin responsável pela aprovação ou rejeição
    private Long adminAprovadorId;

    /** Preenchido pelo admin ao rejeitar o anúncio. Visível apenas ao vendedor. */
    @Column(columnDefinition = "TEXT")
    private String motivoRejeicao;

    /** Promoção — exibe badge "PROMOÇÃO" e preço original riscado na vitrine. */
    @Builder.Default
    private Boolean emPromocao = false;

    // Preço antes da promoção, exibido riscado na vitrine
    private Double precoOriginal;

    // Data/hora em que a promoção expira automaticamente
    private LocalDateTime promocaoExpira;

    // Relacionamento com as avaliações da história
    @OneToMany(mappedBy = "isbnOriginalNoAto", targetEntity = AvaliacaoLivro.class)
    @JsonIgnore
    private List<AvaliacaoLivro> avaliacoes;

    // Obra literária (título canônico) à qual este exemplar pertence
    @ManyToOne
    @JoinColumn(name = "obra_id")
    @JsonIgnore
    private Obra obra;
}