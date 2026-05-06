package umc.exs.DTOs.compra;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaDesejosDTO {
    private Long id;
    private String isbn;
    private LocalDateTime dataAdicao;
    private boolean preReservaAtiva;
}
