package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.DTOs.livro.LivroExibicaoDTO;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroAnuncio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String autor;
    private String isbn;

    @Column(name = "fotos_urls", columnDefinition = "TEXT") 
    @Builder.Default
    private String fotosUrls = "[]"; 
    
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