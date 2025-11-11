package umc.exs.model.dtos.interfaces;

import java.time.YearMonth;

import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;

public interface CartaoConvertible {

    // Método de conversão para Entidade 
    Cartao toEntity(); 

    CartaoDTO fromEntity(Cartao cartao);
    
    // Métodos Getters (Contrato de dados)
    Long getId();
    String getNumero();
    String getBandeira();
    String getNomeTitular();
    YearMonth getValidade(); 
    String getCpfTitular();
}