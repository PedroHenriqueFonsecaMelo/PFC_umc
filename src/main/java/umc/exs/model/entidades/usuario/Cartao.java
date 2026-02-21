package umc.exs.model.entidades.usuario;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data // Inclui @Getter, @Setter, @ToString, @RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id") // Usa apenas o ID para comparação
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    @Column
    private String bandeira;

    @Column(nullable = false)
    private String nomeTitular;

    @Column
    private String validade;

    @Column(nullable = false)
    private String cpfTitular;

    // MappedBy indica que a relação é gerenciada pelo campo 'cartoes' na classe Cliente
    @ManyToMany(mappedBy = "cartoes")
    private Set<Cliente> clientes = new HashSet<>();

}