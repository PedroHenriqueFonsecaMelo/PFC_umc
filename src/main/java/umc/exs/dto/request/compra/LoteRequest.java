package umc.exs.dto.request.compra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.dto.request.livro.LivroItemRequest;

import java.util.List;

/**
 * DTO usado pelo vendedor ao submeter um lote de livros para venda.
 * Contém a lista de itens com seus dados individuais e fotos para análise do admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequest {

    // Lista de livros do lote; cada item é validado individualmente pelo LivroItemRequest
    @NotEmpty(message = "A lista de livros não pode estar vazia")
    @Valid
    private List<LivroItemRequest> livros;
}
