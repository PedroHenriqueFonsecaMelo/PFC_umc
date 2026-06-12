package umc.exs.model.enums;

/**
 * Define os estados de uma solicitação de cancelamento de pedido no sistema.
 */
public enum StatusSolicitacao {

    // Solicitação aguardando análise do admin.
    PENDENTE("Pendente"),
    // Cancelamento aprovado com estorno de tokens.
    APROVADO("Aprovado"),
    // Cancelamento negado pelo admin com comentário.
    RECUSADO("Recusado");

    private final String descricao;

    StatusSolicitacao(String descricao) {
        this.descricao = descricao;
    }

    /** Retorna a descrição legível do status para exibição na interface. */
    public String getDescricao() {
        return descricao;
    }
}
