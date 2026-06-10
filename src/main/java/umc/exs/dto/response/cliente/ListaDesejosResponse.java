package umc.exs.dto.response.cliente;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaDesejosResponse {
    private Long id;
    private String googleBookId;
    private String openLibraryWorkId;
    private String isbn;
    private String titulo;
    private String autor;
    private LocalDateTime dataAdicao;
    private boolean preReservaAtiva;
}
