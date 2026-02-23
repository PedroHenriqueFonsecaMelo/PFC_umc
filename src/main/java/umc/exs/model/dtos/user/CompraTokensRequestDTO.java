package umc.exs.model.dtos.user;

import lombok.Data;

@Data
public class CompraTokensRequestDTO {
    private Double valor;
    private String numeroCartao;
    private String metodoPagamento;
}