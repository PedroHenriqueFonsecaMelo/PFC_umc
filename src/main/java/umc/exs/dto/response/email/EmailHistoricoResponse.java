package umc.exs.dto.response.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailHistoricoResponse {
    private Long id;
    private String assunto;
    private String corpo;
    private String filtro;
    private int quantidadeDestinatarios;
    private String dataRegistro;
    private String agendadoPara;
    private String tipo;
}
