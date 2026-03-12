package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.entidades.foundation.enums.EstadoLivro;
import umc.exs.model.entidades.usuario.Cliente;

// Entidade para o Anúncio do Livro
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
    private String fotoUrl;
    
    @ManyToOne
    private Cliente vendedor;

    private LocalDateTime dataAnuncio;
    
    // Status de controle
    @Builder.Default
    private Boolean aprovado = false; 

    // Estes campos são nulos até que o admin os defina
    private Double precoAprovado; 
    
    @Enumerated(EnumType.STRING)
    private EstadoLivro estadoAprovado; 
    
    private String comentarioAprovacao;
    private LocalDateTime dataAprovacao;
    private Long adminAprovadorId;
}
