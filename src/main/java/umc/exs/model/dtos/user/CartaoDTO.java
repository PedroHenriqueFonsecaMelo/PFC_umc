package umc.exs.model.dtos.user;

import umc.exs.model.entidades.usuario.Cartao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.dtos.interfaces.CartaoConvertible;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartaoDTO implements CartaoConvertible {
    private Long id;
    private String numero;
    private String bandeira;
    private String nomeTitular;
    private String validade;
    private String cvv;
    private boolean preferencial;


    public static CartaoDTO fromEntity(Cartao c) {
        if (c == null)
            return null;
        CartaoDTO dto = new CartaoDTO();
        dto.id = c.getId();
        dto.numero = c.getNumero();
        dto.bandeira = c.getBandeira();
        dto.nomeTitular = c.getNomeTitular();
        dto.validade = c.getValidade();
        dto.cvv = c.getCvv();
        dto.preferencial = c.isPreferencial();
        return dto;
    }

    @Override
    public Cartao toEntity() {
        Cartao c = new Cartao();
        c.setId(this.id);
        c.setNumero(this.numero);
        c.setBandeira(this.bandeira);
        c.setNomeTitular(this.nomeTitular);
        c.setValidade(this.validade);
        c.setCvv(this.cvv);
        c.setPreferencial(this.preferencial);
        return c;
    }

    
}
