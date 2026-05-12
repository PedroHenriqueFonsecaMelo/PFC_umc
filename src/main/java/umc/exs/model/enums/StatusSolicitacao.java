package umc.exs.model.enums;

public enum StatusSolicitacao {

    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    RECUSADO("Recusado");

    private final String descricao;

    StatusSolicitacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
