package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO compartilhado para criação e atualização de endereços do cliente.
 * Utilizado no cadastro, no checkout e no perfil, com preenchimento automático via ViaCEP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoShared {

    // Útil no Response. No PUT/PATCH serve para o Service saber qual endereço alterar
    private Long id;

    // País do endereço
    private String pais;

    // CEP no formato 00000-000 ou 00000000; preenchido automaticamente via ViaCEP
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "O CEP deve estar em um formato válido (Ex: 00000-000 ou 00000000)")
    private String cep;

    // Estado (UF) do endereço
    private String estado;

    // Cidade do endereço
    private String cidade;

    // Logradouro; máximo 255 caracteres
    @Size(max = 255, message = "A rua não pode ter mais de 255 caracteres")
    private String rua;

    // Bairro do endereço
    private String bairro;

    // Número do imóvel
    private String numero;

    // Informação adicional do endereço (ex: apto, bloco); máximo 100 caracteres
    @Size(max = 100, message = "O complemento não pode ter mais de 100 caracteres")
    private String complemento;

    // Tipo do imóvel (ex: casa, apartamento, comercial)
    private String tipoResidencia;
}
