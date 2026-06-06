package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoShared {

    // Útil no Response. No PUT/PATCH serve para o Service saber qual endereço alterar
    private Long id; 

    private String pais;

    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "O CEP deve estar em um formato válido (Ex: 00000-000 ou 00000000)")
    private String cep;

    private String estado;

    private String cidade;

    @Size(max = 255, message = "A rua não pode ter mais de 255 caracteres")
    private String rua;

    private String bairro;

    private String numero;

    @Size(max = 100, message = "O complemento não pode ter mais de 100 caracteres")
    private String complemento;

    private String tipoResidencia;
}