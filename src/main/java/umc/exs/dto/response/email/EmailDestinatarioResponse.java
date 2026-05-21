package umc.exs.dto.response.email;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailDestinatarioResponse {
    private Long id;
    private String nome;
    private String email;
    private Double saldoTokens;
    private Integer xpTotal;
    private String dataCriacao;
}
