package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.List;

@Data
public class EmailDisparoRequest {
    @NotBlank(message = "O filtro é obrigatório")
    private String filtro;

    @Min(value = 1, message = "O limite deve ser maior que 0")
    private Integer limite;

    /** Usado quando filtro = "emails_especificos": lista de e-mails alvo */
    private List<String> emailsEspecificos;

    @NotBlank(message = "O assunto é obrigatório")
    @Size(max = 200, message = "O assunto não pode ter mais de 200 caracteres")
    private String assunto;

    @NotBlank(message = "O corpo do email é obrigatório")
    @Size(max = 5000, message = "O corpo do email não pode ter mais de 5000 caracteres")
    private String corpo;

    private String agendamento;
}
