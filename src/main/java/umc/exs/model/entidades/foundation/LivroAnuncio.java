package umc.exs.model.entidades.foundation;

import java.time.LocalDateTime;


import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.entidades.foundation.enums.EstadoLivro;
import umc.exs.model.entidades.usuario.Cliente;

// Entidade para o Anúncio do Livro
@Entity
@Builder
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LivroAnuncio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String autor;
    private String isbn;
    private Double precoTokens;
    private String fotoUrl; // Caminho da imagem salva

    @Enumerated(EnumType.STRING)
    private EstadoLivro estado; 

    @ManyToOne
    private Cliente vendedor;

    private LocalDateTime dataAnuncio;
}
