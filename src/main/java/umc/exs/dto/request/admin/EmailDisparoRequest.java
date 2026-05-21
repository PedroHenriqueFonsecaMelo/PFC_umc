package umc.exs.dto.request.admin;

import lombok.Data;

@Data
public class EmailDisparoRequest {
    private String filtro;
    private Integer limite;
    private String assunto;
    private String corpo;
    private String agendamento;
}
