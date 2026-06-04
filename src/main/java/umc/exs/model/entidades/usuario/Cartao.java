package umc.exs.model.entidades.usuario;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import com.fasterxml.jackson.annotation.JsonProperty;
import umc.exs.converter.CpfConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Entity

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "clientes")
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Convert(converter = CpfConverter.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String numero;

    @Column(nullable = false)
    private String bandeira;

    @Column(nullable = false)
    private String nomeTitular;

    @Column(nullable = false)
    private String validade;

    @Column(nullable = false, length = 255)
    @Convert(converter = CpfConverter.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cpfTitular;

    // Lado inverso da relação Many-to-Many
    @ManyToMany(mappedBy = "cartoes")
    @Builder.Default
    private Set<Cliente> clientes = new HashSet<>();
}