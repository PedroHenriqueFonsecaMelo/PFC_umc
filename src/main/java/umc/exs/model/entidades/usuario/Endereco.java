package umc.exs.model.entidades.usuario;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.github.manoelcampos.dtogen.DTO;

@Entity
@DTO
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "clientes")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O país é obrigatório")
    private String pais;

    @NotBlank(message = "O CEP é obrigatório")
    @Column(nullable = false, length = 9)
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "O CEP deve estar em um formato válido")
    private String cep;

    @NotBlank(message = "O estado é obrigatório")
    private String estado;

    @NotBlank(message = "A cidade é obrigatória")
    private String cidade;

    @NotBlank(message = "A rua é obrigatória")
    @Size(max = 255, message = "A rua não pode ter mais de 255 caracteres")
    private String rua;

    @NotBlank(message = "O bairro é obrigatório")
    private String bairro;

    @NotBlank(message = "O número é obrigatório")
    private String numero;

    @Size(max = 100, message = "O complemento não pode ter mais de 100 caracteres")
    private String complemento;

    private String tipoResidencia;

    // Lado inverso da relação Many-to-Many
    @ManyToMany(mappedBy = "enderecos")
    @Builder.Default
    private Set<Cliente> clientes = new HashSet<>();
}