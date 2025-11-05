package umc.exs.model.daos.mappers;

import umc.exs.model.dtos.interfaces.CartaoConvertible;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;

public class CartaoMapper {
    
    public static CartaoDTO fromEntity(Cartao cartao) {
        if (cartao == null) {
            return null;
        }
        return CartaoDTO.fromEntity(cartao);
    }

    public static Cartao toEntity(CartaoConvertible cartao) {
        if (cartao == null) {
            return null;
        }
        return cartao.toEntity();
    }
}