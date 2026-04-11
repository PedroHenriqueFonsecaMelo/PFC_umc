package umc.exs.DTOs.gamificacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com as informações de XP/nível do usuário logado,
 * exibidas no card pessoal da homepage.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeuPerfilGamificacaoDTO {

    private String nome;
    private int xpTotal;
    private String nivel;
    private String badge;
    private int xpProximoNivel; // quantos XP faltam para o próximo nível
    private int posicaoRanking; // posição atual no ranking global

    // detalhamento
    private int xpLivrosAprovados;
    private int xpCompras;
    private int xpAvaliacoes;
}
