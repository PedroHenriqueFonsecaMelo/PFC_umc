package umc.exs.model.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.dtos.interfaces.EnderecoConvertible;
import umc.exs.model.entidades.usuario.Endereco;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EnderecoDTO implements EnderecoConvertible {
    private Long id;
    private String pais;
    private String cep;
    private String estado;
    private String cidade;
    private String rua;
    private String bairro;
    private String numero;
    private String complemento;
    private String tipoResidencia;

    public static EnderecoDTO fromEntity(Endereco e) {
        if (e == null)
            return null;
        EnderecoDTO dto = new EnderecoDTO();
        dto.id = e.getId();
        dto.pais = e.getPais();
        dto.cep = e.getCep();
        dto.estado = e.getEstado();
        dto.cidade = e.getCidade();
        dto.rua = e.getRua();
        dto.bairro = e.getBairro();
        dto.numero = String.valueOf(e.getNumero());
        dto.complemento = e.getComplemento();
        dto.tipoResidencia = e.getTipoResidencia();
        return dto;
    }

    @Override
    public Endereco toEntity() {
        Endereco e = new Endereco();
        e.setId(this.id);
        e.setPais(this.pais);
        e.setCep(this.cep);
        e.setEstado(this.estado);
        e.setCidade(this.cidade);
        e.setRua(this.rua);
        e.setBairro(this.bairro);
        if (this.numero != null)
            e.setNumero(this.numero);
        e.setComplemento(this.complemento);
        e.setTipoResidencia(this.tipoResidencia);
        return e;
    }

}
