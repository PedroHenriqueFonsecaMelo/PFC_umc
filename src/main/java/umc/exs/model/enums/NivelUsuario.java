package umc.exs.model.enums;

/**
 * Define os níveis de gamificação do sistema: Iniciante (0-199 XP),
 * Bronze (200-499 XP), Prata (500-999 XP) e Ouro (1000+ XP).
 */
public enum NivelUsuario {

    // Nível inicial — faixa de 0 a 199 XP.
    INICIANTE("Leitor Iniciante", "", 0, 199),
    // Nível bronze — faixa de 200 a 499 XP.
    BRONZE("Leitor Bronze", "", 200, 499),
    // Nível prata — faixa de 500 a 999 XP.
    PRATA("Leitor Prata", "", 500, 999),
    // Nível ouro — faixa de 1000+ XP.
    OURO("Leitor Ouro", "", 1000, Integer.MAX_VALUE);

    private final String descricao;
    private final String badge;
    private final int xpMinimo;
    private final int xpMaximo;

    NivelUsuario(String descricao, String badge, int xpMinimo, int xpMaximo) {
        this.descricao = descricao;
        this.badge = badge;
        this.xpMinimo = xpMinimo;
        this.xpMaximo = xpMaximo;
    }

    /** Retorna o nome legível do nível. */
    public String getDescricao() {
        return descricao;
    }

    /** Retorna o emoji do badge do nível. */
    public String getBadge() {
        return badge;
    }

    /** Retorna o XP mínimo do nível. */
    public int getXpMinimo() {
        return xpMinimo;
    }

    /** Retorna o XP máximo do nível. */
    public int getXpMaximo() {
        return xpMaximo;
    }

    /**
     * Recebe o XP total do usuário e retorna o nível correspondente;
     * retorna INICIANTE como fallback se nenhum nível for encontrado.
     */
    public static NivelUsuario calcular(int xpTotal) {
        for (NivelUsuario nivel : values()) {
            if (xpTotal >= nivel.xpMinimo && xpTotal <= nivel.xpMaximo) {
                return nivel;
            }
        }
        return INICIANTE;
    }
}
