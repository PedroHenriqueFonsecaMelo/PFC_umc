package umc.exs.DTOs.compra;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ListaDesejosDTO {
    private Long id;
    private String isbn;
    private LocalDateTime dataAdicao;
}
