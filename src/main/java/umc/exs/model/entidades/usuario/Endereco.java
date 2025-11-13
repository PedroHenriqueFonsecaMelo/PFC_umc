package umc.exs.model.entidades.usuario;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data // Inclui @Getter, @Setter, @ToString, @RequiredArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id") // Usa apenas o ID para comparação
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column
    private String pais;
    @Column
    private String cep;
    @Column
    private String estado;
    @Column
    private String cidade;
    @Column
    private String rua;
    @Column
    private String bairro;
    @Column
    private String numero;
    @Column
    private String complemento;
    @Column
    private String tipoResidencia;

    // MappedBy indica que a relação é gerenciada pelo campo 'enderecos' na classe Cliente
    @ManyToMany(mappedBy = "enderecos")
    private Set<Cliente> clientes = new HashSet<>();
}