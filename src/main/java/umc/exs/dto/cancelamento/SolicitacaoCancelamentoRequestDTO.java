package umc.exs.dto.cancelamento;

import umc.exs.model.enums.MotivoCategoria;

public class SolicitacaoCancelamentoRequestDTO {

    private MotivoCategoria motivoCategoria;
    private String motivoDescricao;

    public MotivoCategoria getMotivoCategoria() { return motivoCategoria; }
    public void setMotivoCategoria(MotivoCategoria motivoCategoria) { this.motivoCategoria = motivoCategoria; }

    public String getMotivoDescricao() { return motivoDescricao; }
    public void setMotivoDescricao(String motivoDescricao) { this.motivoDescricao = motivoDescricao; }
}
