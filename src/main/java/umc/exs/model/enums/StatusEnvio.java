package umc.exs.model.enums;

/**
 * Ciclo de vida do envio de um livro comprado.
 * AGUARDANDO_ENVIO → EM_TRANSITO → ENTREGUE
 * CANCELAMENTO_SOLICITADO: aguarda análise do admin.
 * CANCELADO pode ocorrer a qualquer momento antes de ENTREGUE.
 */
public enum StatusEnvio {

    AGUARDANDO_ENVIO("Aguardando Envio"),
    EM_TRANSITO("Em Trânsito"),
    ENTREGUE("Entregue"),
    CANCELAMENTO_SOLICITADO("Cancelamento Solicitado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusEnvio(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
