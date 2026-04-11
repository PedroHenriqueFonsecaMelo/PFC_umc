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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "clientes")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pais;

    @Column(nullable = false, length = 9)
    private String cep;

    private String estado;
    private String cidade;
    private String rua;
    private String bairro;
    private String numero;
    private String complemento;
    private String tipoResidencia;

    // Lado inverso da relação Many-to-Many
    @ManyToMany(mappedBy = "enderecos")
    private Set<Cliente> clientes = new HashSet<>();
}