package umc.exs.dto.response.gamificacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO público de ranking — LGPD-compliant.
 * Nunca expõe: e-mail, CPF, telefone, endereço ou sobrenome completo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingPublicResponse {

    /** Posição no ranking (1-based). */
    private int posicao;

    /** Primeiro nome do usuário. Ex: "João" */
    private String primeiroNome;

    /** Inicial do sobrenome + ponto. Ex: "S." — pode ser vazio. */
    private String inicialSobrenome;

    /** Descrição do nível. Ex: "Leitor Ouro" */
    private String nivel;

    /** Emoji do badge. Ex: "📕" */
    private String badge;

    /** XP acumulado total. */
    private int xpTotal;

    /** XP necessário para o próximo nível (0 = nível máximo). */
    private int xpProximoNivel;

    /** URL da foto de perfil — pode ser null. */
    private String fotoPerfil;
}
