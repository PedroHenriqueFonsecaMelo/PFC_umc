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


/**
 * Representa um cartão de crédito ou débito do cliente, com número e CPF
 * criptografados no banco via AES-256-GCM para conformidade com a LGPD.
 */
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

    // Número do cartão criptografado no banco (LGPD).
    @Column(nullable = false, unique = true)
    @Convert(converter = CpfConverter.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String numero;

    // Bandeira do cartão (Visa, Mastercard, etc.).
    @Column(nullable = false)
    private String bandeira;

    // Nome impresso no cartão.
    @Column(nullable = false)
    private String nomeTitular;

    // Data de validade no formato MM/yy.
    @Column(nullable = false)
    private String validade;

    // CPF do titular criptografado no banco (LGPD).
    @Column(nullable = false, length = 255)
    @Convert(converter = CpfConverter.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cpfTitular;

    // Lado inverso da relação Many-to-Many
    @ManyToMany(mappedBy = "cartoes")
    @Builder.Default
    private Set<Cliente> clientes = new HashSet<>();
}