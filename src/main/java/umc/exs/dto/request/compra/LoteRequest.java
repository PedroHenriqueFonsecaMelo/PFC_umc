package umc.exs.dto.request.compra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.dto.request.livro.LivroItemRequest;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequest {
    @NotEmpty(message = "A lista de livros não pode estar vazia")
    @Valid
    private List<LivroItemRequest> livros;
}
