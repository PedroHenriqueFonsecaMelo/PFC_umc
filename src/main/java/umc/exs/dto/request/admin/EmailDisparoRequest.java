package umc.exs.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.List;

/**
 * DTO usado pelo admin para disparar ou agendar e-mails segmentados para grupos de clientes.
 * Permite filtrar destinatários por segmento, definir assunto, corpo e agendar envio futuro.
 */
@Data
public class EmailDisparoRequest {

    // Segmento de destinatários (ex: todos, top_xp, baixo_saldo, emails_especificos); obrigatório
    @NotBlank(message = "O filtro é obrigatório")
    private String filtro;

    // Quantidade máxima de destinatários a serem incluídos no disparo; opcional
    @Min(value = 1, message = "O limite deve ser maior que 0")
    private Integer limite;

    /** Usado quando filtro = "emails_especificos": lista de e-mails alvo */
    private List<String> emailsEspecificos;

    // Assunto do e-mail a ser enviado; obrigatório e com máximo de 200 caracteres
    @NotBlank(message = "O assunto é obrigatório")
    @Size(max = 200, message = "O assunto não pode ter mais de 200 caracteres")
    private String assunto;

    // Conteúdo HTML ou texto do e-mail; obrigatório e com máximo de 5000 caracteres
    @NotBlank(message = "O corpo do email é obrigatório")
    @Size(max = 5000, message = "O corpo do email não pode ter mais de 5000 caracteres")
    private String corpo;

    // Data e hora para envio futuro no formato ISO-8601 (ex: 2025-12-31T10:00:00); opcional
    private String agendamento;
}
