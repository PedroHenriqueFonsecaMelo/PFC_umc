package umc.exs.model.enums;

public enum MotivoCategoria {

    COMPREI_POR_ENGANO("Comprei por engano"),
    ENCONTREI_MAIS_BARATO("Encontrei mais barato"),
    PRODUTO_NAO_ESPERADO("Produto não era o esperado"),
    OUTRO("Outro motivo"),

    // Motivos de cancelamento pelo administrador
    PRODUTO_NAO_DISPONIVEL("Produto não disponível para envio"),
    DADOS_INCORRETOS("Dados do comprador incorretos ou incompletos"),
    SUSPEITA_FRAUDE("Suspeita de fraude ou abuso"),
    PROBLEMA_LOGISTICA("Problema logístico"),
    PEDIDO_DUPLICADO("Pedido duplicado"),
    DECISAO_ADMINISTRATIVA("Decisão administrativa");

    private final String descricao;

    MotivoCategoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
