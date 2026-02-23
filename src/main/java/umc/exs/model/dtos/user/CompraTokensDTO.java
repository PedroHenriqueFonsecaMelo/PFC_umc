package umc.exs.model.dtos.user;

import lombok.Data;

@Data
public class CompraTokensDTO {
    private Long clienteId;
    private Double valor;
    private String numeroCartao; 
}