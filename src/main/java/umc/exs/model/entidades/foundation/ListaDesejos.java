package umc.exs.model.entidades.foundation;

import jakarta.persistence.*;
import lombok.*;

import umc.exs.model.entidades.usuario.Cliente;

import java.time.LocalDateTime;

/**
 * Representa um item da lista de desejos do cliente identificado pelo ISBN;
 * a constraint única garante que o mesmo ISBN não seja adicionado duas vezes
 * pelo mesmo cliente.
 */
@Entity
@Table(name = "lista_desejos", uniqueConstraints = @UniqueConstraint(columnNames = { "cliente_id", "isbn" }))

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaDesejos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cliente dono da lista de desejos.
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // ISBN do livro desejado.
    @Column(nullable = false)
    private String isbn;

    // Data e hora em que o livro foi adicionado à lista.
    @Column(nullable = false)
    private LocalDateTime dataAdicao;

    /**
     * Quando ativo, compra o livro automaticamente se o cliente tiver saldo ao
     * receber notificação.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean preReservaAtiva = false;
}
