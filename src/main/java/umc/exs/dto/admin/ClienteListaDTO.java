package umc.exs.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClienteListaDTO {
    private Long id;
    private String nome;
    private String email;
    private LocalDateTime dataCadastro;
    private Double saldoTokens;
    private String nivel;
    private long totalCompras;
    private double totalGasto;
    private boolean ativo;
}
