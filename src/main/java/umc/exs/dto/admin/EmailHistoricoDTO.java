package umc.exs.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailHistoricoDTO {
    private Long id;
    private String assunto;
    private String corpo;
    private String filtro;
    private int quantidadeDestinatarios;
    private String dataRegistro;
    private String agendadoPara;
    private String tipo;
}
