package umc.exs.DTOs.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailDestinatarioDTO {
    private Long id;
    private String nome;
    private String email;
    private Double saldoTokens;
    private Integer xpTotal;
    private String dataCriacao;
}
