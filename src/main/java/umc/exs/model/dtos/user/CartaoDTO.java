package umc.exs.model.dtos.user;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;

import umc.exs.model.entidades.usuario.Cartao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.daos.mappers.CartaoMapper;
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

    @DateTimeFormat(pattern = "MM/yyyy")
    private YearMonth validade; // Tipo YearMonth para tratamento Java
    private String cpfTitular; 
    
    /** 
     * @return Cartao
     */
    @Override
    public Cartao toEntity() {
        return CartaoMapper.toEntity(this);
    }

    /** 
     * @param cartao
     * @return CartaoDTO
     */
    @Override
    public CartaoDTO fromEntity (Cartao cartao){
        return CartaoMapper.fromEntity(cartao);
    }

    /** 
     * @return String
     */
    @Override
    public String toString() {
        return "CartaoDTO [id=" + id + ", numero=" + numero + ", bandeira=" + bandeira + ", nomeTitular=" + nomeTitular
                + ", validade=" + validade + ", cpfTitular=" + cpfTitular + "]";
    }

    
}