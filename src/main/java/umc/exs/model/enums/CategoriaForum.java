package umc.exs.model.enums;

/**
 * Define as categorias disponíveis para criação de tópicos no fórum da
 * plataforma.
 */
public enum CategoriaForum {

    // Tópicos de resenhas de livros.
    RESENHAS("Resenhas"),
    // Tópicos de dúvidas sobre livros ou a plataforma.
    DUVIDAS("Dúvidas"),
    // Tópicos de recomendações de leitura.
    RECOMENDACOES("Recomendações"),
    // Tópicos de discussão geral.
    GERAL("Geral");

    private final String descricao;

    CategoriaForum(String descricao) {
        this.descricao = descricao;
    }

    /** Retorna o nome legível da categoria para exibição na interface. */
    public String getDescricao() {
        return descricao;
    }
}
