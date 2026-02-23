package umc.exs.model.dtos.user;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnderecoDTO {
    
    private Long id;
    private String rua;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    private String pais;
    private String complemento;
    private String tipoResidencia;
}