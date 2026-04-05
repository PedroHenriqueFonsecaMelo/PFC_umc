package umc.exs.model.enums;

public enum NivelUsuario {

    INICIANTE("Leitor Iniciante", "📗", 0, 199),
    BRONZE("Leitor Bronze", "📘", 200, 499),
    PRATA("Leitor Prata", "📙", 500, 999),
    OURO("Leitor Ouro", "📕", 1000, Integer.MAX_VALUE);

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

    public String getDescricao() {
        return descricao;
    }

    public String getBadge() {
        return badge;
    }

    public int getXpMinimo() {
        return xpMinimo;
    }

    public int getXpMaximo() {
        return xpMaximo;
    }

    public static NivelUsuario calcular(int xpTotal) {
        for (NivelUsuario nivel : values()) {
            if (xpTotal >= nivel.xpMinimo && xpTotal <= nivel.xpMaximo) {
                return nivel;
            }
        }
        return INICIANTE;
    }
}
