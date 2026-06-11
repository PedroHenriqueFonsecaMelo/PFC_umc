package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.enums.StatusEnvio;

/**
 * DTO enviado pelo admin para atualizar o status de envio de um pedido
 * e registrar o código de rastreio da transportadora.
 */
@Getter
@Setter
@NoArgsConstructor
public class AtualizarEnvioRequest {

    // Novo status do pedido (ex: EM_TRANSITO, ENTREGUE); obrigatório
    @NotNull(message = "O status de envio é obrigatório.")
    private StatusEnvio statusEnvio;

    // Código de rastreio fornecido pela transportadora; opcional
    private String codigoRastreio;
}
