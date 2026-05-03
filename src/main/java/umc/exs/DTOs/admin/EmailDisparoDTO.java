package umc.exs.DTOs.admin;

import lombok.Data;

@Data
public class EmailDisparoDTO {
    private String filtro;
    private Integer limite;
    private String assunto;
    private String corpo;
    private String agendamento;
}
