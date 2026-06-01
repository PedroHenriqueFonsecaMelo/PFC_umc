package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import umc.exs.model.enums.MotivoCategoria;

public class CancelamentoRequest {

    @NotNull(message = "A categoria do motivo é obrigatória")
    private MotivoCategoria motivoCategoria;

    @NotBlank(message = "A descrição do motivo é obrigatória")
    @Size(max = 500, message = "A descrição não pode ter mais de 500 caracteres")
    private String motivoDescricao;

    public MotivoCategoria getMotivoCategoria() {
        return motivoCategoria;
    }

    public void setMotivoCategoria(MotivoCategoria motivoCategoria) {
        this.motivoCategoria = motivoCategoria;
    }

    public String getMotivoDescricao() {
        return motivoDescricao;
    }

    public void setMotivoDescricao(String motivoDescricao) {
        this.motivoDescricao = motivoDescricao;
    }
}
