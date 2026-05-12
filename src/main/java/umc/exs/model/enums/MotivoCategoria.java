package umc.exs.model.enums;

public enum MotivoCategoria {

    COMPREI_POR_ENGANO("Comprei por engano"),
    ENCONTREI_MAIS_BARATO("Encontrei mais barato"),
    PRODUTO_NAO_ESPERADO("Produto não era o esperado"),
    OUTRO("Outro motivo");

    private final String descricao;

    MotivoCategoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
