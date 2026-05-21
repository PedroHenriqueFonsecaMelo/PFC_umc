package umc.exs.dto.request.compra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.dto.request.livro.LivroItemRequest;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequest {
    private List<LivroItemRequest> livros;
}
