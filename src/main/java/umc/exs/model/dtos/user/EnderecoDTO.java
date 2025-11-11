package umc.exs.model.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.dtos.interfaces.EnderecoConvertible;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.model.daos.mappers.EnderecoMapper; // Importado para delegação

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EnderecoDTO implements EnderecoConvertible {
    
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

    // O DTO delega a conversão ao Mapper
    @Override
    public Endereco toEntity() {
        return EnderecoMapper.toEntity(this);
    }
    @Override
    public EnderecoDTO fromEntity (Endereco endereco){
        return EnderecoMapper.fromEntity(endereco);
    }
}