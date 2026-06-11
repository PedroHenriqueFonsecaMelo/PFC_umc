package umc.exs.dto.response.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta que representa um destinatário filtrado para disparo de e-mail pelo admin.
 * Inclui dados de saldo de tokens e XP total usados como critérios de segmentação.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDestinatarioResponse {
    private Long id;
    private String nome;
    private String email;
    private Double saldoTokens;
    private Integer xpTotal;
    private String dataCriacao;
}
