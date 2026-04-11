package umc.exs.DTOs.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoDTO {

    /**
     * ID único endereço banco.
     */
    private Long id;

    /**
     * Nome rua principal.
     */
    private String rua;

    /**
     * Número prédio/casa.
     */
    private String numero;

    /**
     * Bairro/localidade.
     */
    private String bairro;

    /**
     * Cidade residência.
     */
    private String cidade;

    /**
     * Estado UF (SP, RJ etc).
     */
    private String estado;

    /**
     * CEP formato string.
     */
    private String cep;

    /**
     * País (default BR).
     */
    private String pais;

    /**
     * Complemento apt/bloco.
     */
    private String complemento;

    /**
     * Tipo casa/apto etc.
     */
    private String tipoResidencia;
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * DTO dados endereço cliente cadastro/perfil.
 * Lombok Data, campos essenciais rua/num/cep/estado.
 * Usado forms frontend, mappers entidades.
 * Sem validações anotadas (service layer).
 */
