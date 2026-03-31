package umc.exs.DTOs.gamificacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO retornado pelo endpoint de ranking.
 * Expõe apenas os dados necessários para exibição pública.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingItemDTO {

    private int posicao;
    private String nome;
    private int xpTotal;
    private String nivel;
    private String badge;

    // XP por categoria (detalhamento opcional no card)
    private int xpLivrosAprovados;
    private int xpCompras;
    private int xpAvaliacoes;
}
