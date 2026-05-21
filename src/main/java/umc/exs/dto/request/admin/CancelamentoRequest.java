package umc.exs.dto.request.admin;

import umc.exs.model.enums.MotivoCategoria;

public class CancelamentoRequest {

    private MotivoCategoria motivoCategoria;
    private String motivoDescricao;

    public MotivoCategoria getMotivoCategoria() { return motivoCategoria; }
    public void setMotivoCategoria(MotivoCategoria motivoCategoria) { this.motivoCategoria = motivoCategoria; }

    public String getMotivoDescricao() { return motivoDescricao; }
    public void setMotivoDescricao(String motivoDescricao) { this.motivoDescricao = motivoDescricao; }
}
