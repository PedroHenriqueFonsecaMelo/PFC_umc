package umc.exs.DTOs.compra;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PixResponseDTO {
    private String copiaECola;
    private String qrCodeBase64;
    private String status;

    private String pagamentoId;
}
