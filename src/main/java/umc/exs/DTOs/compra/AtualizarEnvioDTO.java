package umc.exs.DTOs.compra;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.enums.StatusEnvio;

/**
 * DTO para atualização de status de envio pelo administrador.
 */
@Getter
@Setter
@NoArgsConstructor
public class AtualizarEnvioDTO {

    @NotNull(message = "O status de envio é obrigatório.")
    private StatusEnvio statusEnvio;

    private String codigoRastreio;
}
