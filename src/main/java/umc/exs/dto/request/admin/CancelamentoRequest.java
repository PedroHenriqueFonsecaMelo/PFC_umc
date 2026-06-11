package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import umc.exs.model.enums.MotivoCategoria;

/**
 * DTO enviado pelo cliente ao solicitar o cancelamento de um pedido.
 * Informa a categoria do motivo e uma descrição detalhada da solicitação.
 */
public class CancelamentoRequest {

    // Categoria do motivo do cancelamento (ex: PRODUTO_DANIFICADO, ARREPENDIMENTO); obrigatória
    @NotNull(message = "A categoria do motivo é obrigatória")
    private MotivoCategoria motivoCategoria;

    // Descrição livre do motivo pelo cliente; obrigatória e com máximo de 500 caracteres
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
