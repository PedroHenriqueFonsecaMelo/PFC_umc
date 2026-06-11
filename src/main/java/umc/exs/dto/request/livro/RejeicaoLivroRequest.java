package umc.exs.dto.request.livro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO enviado pelo admin ao rejeitar um livro pendente de aprovação.
 * Informa o estado de conservação avaliado e o motivo da rejeição para notificar o vendedor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejeicaoLivroRequest {

    // Estado de conservação avaliado pelo admin no momento da rejeição; obrigatório
    @NotBlank(message = "O estado é obrigatório")
    private String estado;

    // Motivo da rejeição exibido ao vendedor para que possa corrigir o anúncio; máximo 500 caracteres
    @NotBlank(message = "O comentário é obrigatório")
    @Size(max = 500, message = "O comentário não pode ter mais de 500 caracteres")
    private String comentario;
}
