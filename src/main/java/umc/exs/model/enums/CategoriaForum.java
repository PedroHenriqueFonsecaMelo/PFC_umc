package umc.exs.model.enums;

public enum CategoriaForum {

    RESENHAS("Resenhas"),
    DUVIDAS("Dúvidas"),
    RECOMENDACOES("Recomendações"),
    GERAL("Geral");

    private final String descricao;

    CategoriaForum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
