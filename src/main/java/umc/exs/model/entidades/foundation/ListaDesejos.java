package umc.exs.model.entidades.foundation;

import jakarta.persistence.*;
import lombok.*;
import umc.exs.model.entidades.usuario.Cliente;

import java.time.LocalDateTime;

@Entity
@Table(name = "lista_desejos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "isbn"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaDesejos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false)
    private String isbn;

    @Column(nullable = false)
    private LocalDateTime dataAdicao;
}
