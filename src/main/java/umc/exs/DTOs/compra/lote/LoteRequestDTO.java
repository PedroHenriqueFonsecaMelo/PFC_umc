package umc.exs.dtos.compra.lote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.dtos.livro.LivroItemDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequestDTO {
    private List<LivroItemDTO> livros;
}
